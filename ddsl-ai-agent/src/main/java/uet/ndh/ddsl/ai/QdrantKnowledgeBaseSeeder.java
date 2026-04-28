package uet.ndh.ddsl.ai;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Seeds the Qdrant vector store with DDSL specification rules, few-shot examples,
 * and fuzzy-input mapping patterns on application startup.
 * <p>
 * Documents are loaded from Markdown files under {@code classpath:dsl-spec/} with
 * YAML front matter for metadata. Uses text-embedding-3-small (1536 dimensions).
 * <p>
 * Collection name: {@code ddsl-knowledge-base-v3} (v3 for 1536-dim embeddings)
 *
 * @see org.springframework.ai.openai.OpenAiEmbeddingModel
 */
@Component
public class QdrantKnowledgeBaseSeeder {

    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String SPEC_RESOURCE_PATTERN = "classpath:dsl-spec/**/*.md";
    
    @Value("${spring.ai.vectorstore.qdrant.collection-name:ddsl-knowledge-base-v3}")
    private String collectionName;

    private final VectorStore vectorStore;
    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public QdrantKnowledgeBaseSeeder(VectorStore vectorStore, QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.qdrantClient = qdrantClient;
        this.embeddingModel = embeddingModel;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedKnowledgeBase() {
        log.info("Checking Qdrant collection '{}' for existing documents...", collectionName);

        long initialCount = 0;
        try {
            initialCount = qdrantClient.countAsync(collectionName).get();
            log.info("Current document count in Qdrant: {}", initialCount);
        } catch (Exception e) {
            log.warn("Could not get current document count: {}", e.getMessage());
        }

        List<Document> documents = new ArrayList<>();

        try {
            Resource[] resources = resolver.getResources(SPEC_RESOURCE_PATTERN);
            log.info("Found {} Markdown spec files", resources.length);

            for (Resource resource : resources) {
                try {
                    Document doc = parseMarkdownDocument(resource);
                    if (doc != null) {
                        documents.add(doc);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse spec file: {} — {}", resource.getFilename(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan dsl-spec directory", e);
            return;
        }

        if (documents.isEmpty()) {
            log.warn("No documents found to seed. Check classpath:dsl-spec/ directory.");
            return;
        }

        // Filter out documents that already exist in Qdrant
        List<Document> newDocuments = filterExistingDocuments(documents);

        if (newDocuments.isEmpty()) {
            log.info("All {} documents already exist in Qdrant. No embedding needed.", documents.size());

            try {
                log.info("Testing embedding model with sample text...");
                var embeddingResponse = embeddingModel.embed("test entity aggregate");
                log.info("Embedding model responded with {} dimensions", embeddingResponse.length);
            } catch (Exception e) {
                log.error("Embedding model test failed: {}", e.getMessage(), e);
            }

            try {
                log.info("Testing similarity search with query: 'entity'");
                org.springframework.ai.vectorstore.SearchRequest testRequest = org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query("entity")
                        .topK(3)
                        .similarityThreshold(0.0)
                        .build();
                var testResults = vectorStore.similaritySearch(testRequest);
                log.info("Test search returned {} documents", testResults.size());
                testResults.forEach(doc -> log.info("  Found: id={}, score={}", doc.getId(), doc.getMetadata().getOrDefault("distance", "N/A")));
            } catch (Exception e) {
                log.error("Test search failed: {}", e.getMessage(), e);
            }

            try {
                log.info("Checking if documents have vectors by retrieving first document...");
                var firstDocId = documents.get(0).getId();
                var points = qdrantClient.retrieveAsync(
                        collectionName,
                        List.of(Points.PointId.newBuilder().setUuid(firstDocId).build()),
                        false,
                        true,
                        null
                ).get();
                if (!points.isEmpty()) {
                    var point = points.get(0);
                    boolean hasVectors = point.hasVectors();
                    log.info("First document (id={}) has vectors: {}", firstDocId, hasVectors);
                    if (!hasVectors) {
                        log.error("CRITICAL: Documents exist but have NO EMBEDDINGS! The embedding model failed during initial seeding.");
                        log.error("Solution: Delete the Qdrant collection and restart to re-seed with embeddings.");
                        log.error("Run: curl -X DELETE http://localhost:6333/collections/ddsl-knowledge-base-v3");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to check document vectors: {}", e.getMessage());
            }

            return;
        }

        log.info("Found {} new documents to embed ({} already exist)", 
                newDocuments.size(), documents.size() - newDocuments.size());
        
        try {
            vectorStore.add(newDocuments);
            log.info("Successfully embedded {} new documents into Qdrant collection '{}'",
                    newDocuments.size(), collectionName);

            Thread.sleep(1000);
            long newCount = qdrantClient.countAsync(collectionName).get();
            log.info("Document count after seeding: {} (added {})", newCount, newCount - initialCount);
        } catch (Exception e) {
            log.error("Failed to embed documents into Qdrant: {}", e.getMessage(), e);
        }
        log.info("Using embedding model: text-embedding-3-small (1536 dimensions)");
    }

    private List<Document> filterExistingDocuments(List<Document> documents) {
        // Extract all document IDs
        List<String> docIds = documents.stream()
                .map(Document::getId)
                .filter(Objects::nonNull)
                .toList();

        if (docIds.isEmpty()) {
            return documents;
        }

        // Check which documents already exist in Qdrant
        Set<String> existingIds = new HashSet<>();
        try {
            // Convert String IDs to PointId objects for Qdrant
            List<Points.PointId> pointIds = docIds.stream()
                    .map(id -> Points.PointId.newBuilder().setUuid(id).build())
                    .toList();

            var retrievedPoints = qdrantClient.retrieveAsync(
                    collectionName,
                    pointIds,
                    true,
                    false,
                    null
            ).get();

            // Collect existing IDs
            for (Points.RetrievedPoint point : retrievedPoints) {
                if (point.hasId()) {
                    existingIds.add(point.getId().getUuid());
                }
            }

            log.debug("Found {} existing documents in Qdrant", existingIds.size());

        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to check existing documents in Qdrant: {}. Will proceed with all documents.", e.getMessage());
            log.debug("Exception details: ", e);
            // If we can't check, return all documents to be safe
            return documents;
        }

        // Filter out existing documents
        return documents.stream()
                .filter(doc -> !existingIds.contains(doc.getId()))
                .collect(Collectors.toList());
    }

    private Document parseMarkdownDocument(Resource resource) throws IOException {
        String raw = resource.getContentAsString(StandardCharsets.UTF_8).strip();

        if (!raw.startsWith(FRONT_MATTER_DELIMITER)) {
            log.warn("No front matter found in: {}", resource.getFilename());
            return null;
        }

        int secondDelimiter = raw.indexOf(FRONT_MATTER_DELIMITER, FRONT_MATTER_DELIMITER.length());
        if (secondDelimiter < 0) {
            log.warn("Malformed front matter in: {}", resource.getFilename());
            return null;
        }

        String yamlBlock = raw.substring(FRONT_MATTER_DELIMITER.length(), secondDelimiter).strip();
        String content = raw.substring(secondDelimiter + FRONT_MATTER_DELIMITER.length()).strip();

        if (content.isEmpty()) {
            log.warn("Empty content body in: {}", resource.getFilename());
            return null;
        }

        Yaml yaml = new Yaml();
        Map<String, Object> frontMatter = yaml.load(yamlBlock);
        if (frontMatter == null) {
            frontMatter = Map.of();
        }

        String rawId = String.valueOf(frontMatter.getOrDefault("id", resource.getFilename()));
        String id = UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)).toString();

        Map<String, Object> metadata = new HashMap<>(frontMatter);
        metadata.remove("id");
        metadata.put("original_id", rawId);
        metadata.put("source", "ddsl-knowledge-base");
        metadata.put("version", "3.0");
        metadata.put("file", resource.getFilename());
        metadata.put("embedding_model", "text-embedding-3-small");
        metadata.put("dimensions", 1536);

        log.debug("Parsed document '{}' from {} ({} chars)", id, resource.getFilename(), content.length());
        return new Document(id, content, metadata);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QdrantKnowledgeBaseSeeder.class);
}
