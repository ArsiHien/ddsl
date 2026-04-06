package uet.ndh.ddsl.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Seeds the Qdrant vector store with DDSL specification rules, few-shot examples,
 * and fuzzy-input mapping patterns on application startup.
 * <p>
 * Documents are loaded from Markdown files under {@code classpath:dsl-spec/} with
 * YAML front matter for metadata. This approach separates knowledge content from
 * application code, making it easy to add/edit documents without recompilation.
 *
 * <h3>Markdown file format:</h3>
 * <pre>
 * ---
 * id: ddsl-syntax-001
 * category: SYNTAX_RULE
 * subcategory: aggregate
 * dsl_construct: Aggregate
 * complexity: basic
 * ---
 * Content text for embedding goes here...
 * </pre>
 *
 * <h3>Scanned directories:</h3>
 * <ul>
 *   <li>{@code dsl-spec/syntax/}       — SYNTAX_RULE documents</li>
 *   <li>{@code dsl-spec/few-shot/}     — FEW_SHOT examples</li>
 *   <li>{@code dsl-spec/fuzzy/}        — FUZZY_MAPPING rules</li>
 *   <li>{@code dsl-spec/patterns/}     — DDD_PATTERN guides</li>
 *   <li>{@code dsl-spec/constraints/}  — CONSTRAINT references</li>
 * </ul>
 */
@Component
public class QdrantKnowledgeBaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(QdrantKnowledgeBaseSeeder.class);
    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String SPEC_RESOURCE_PATTERN = "classpath:dsl-spec/**/*.md";

    private final VectorStore vectorStore;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public QdrantKnowledgeBaseSeeder(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedKnowledgeBase() {
        log.info("Seeding Qdrant knowledge base from classpath:dsl-spec/**/*.md ...");

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

        vectorStore.add(documents);
        log.info("Seeded {} documents into Qdrant", documents.size());
    }

    /**
     * Parses a Markdown file with YAML front matter into a Spring AI {@link Document}.
     *
     * @param resource the classpath resource to parse
     * @return a Document with content and metadata, or {@code null} if parsing fails
     */
    private Document parseMarkdownDocument(Resource resource) throws IOException {
        String raw = resource.getContentAsString(StandardCharsets.UTF_8).strip();

        // Split front matter from content
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

        // Parse YAML front matter
        Yaml yaml = new Yaml();
        Map<String, Object> frontMatter = yaml.load(yamlBlock);
        if (frontMatter == null) {
            frontMatter = Map.of();
        }

        // Extract document id (required) — convert to UUID because Qdrant requires it
        String rawId = String.valueOf(frontMatter.getOrDefault("id", resource.getFilename()));
        String id = UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)).toString();

        // Build metadata map
        Map<String, Object> metadata = new HashMap<>(frontMatter);
        metadata.remove("id"); // id is used as the Document ID, not metadata
        metadata.put("original_id", rawId); // preserve the human-readable id
        metadata.put("source", "ddsl-knowledge-base");
        metadata.put("version", "2.0");
        metadata.put("file", resource.getFilename());

        log.debug("Parsed document '{}' from {} ({} chars)", id, resource.getFilename(), content.length());
        return new Document(id, content, metadata);
    }
}
