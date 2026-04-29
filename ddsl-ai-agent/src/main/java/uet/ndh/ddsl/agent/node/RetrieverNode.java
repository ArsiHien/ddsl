package uet.ndh.ddsl.agent.node;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DdslState;
import uet.ndh.ddsl.agent.cache.RetrievalCache;

import java.util.*;

/**
 * <b>Retriever Agent</b> — semantic search with quality assessment.
 * <p>
 * Queries Qdrant and assesses retrieval quality. If quality is below threshold,
 * retries with adjusted parameters (lower similarity threshold, higher top-k).
 * <p>
 * Quality Score Calculation:
 * <ul>
 *   <li>Base: Average similarity score of retrieved documents</li>
 *   <li>Bonus: +0.1 if documents from multiple categories</li>
 *   <li>Bonus: +0.1 if total content length > 500 chars</li>
 * </ul>
 * <p>
 * Minimum quality threshold: 0.6 (configurable)
 * Max retries: 2
 */
@Component
@Slf4j
public class RetrieverNode implements NodeAction<DdslState> {

    private final VectorStore vectorStore;
    private final RetrievalCache cache;

    @Value("${ddsl.agent.retriever.top-k:4}")
    private int topK;

    @Value("${ddsl.agent.retriever.similarity-threshold:0.45}")
    private double similarityThreshold;

    @Value("${ddsl.agent.retriever.min-quality:0.6}")
    private double minQualityThreshold;

    public RetrieverNode(VectorStore vectorStore, RetrievalCache cache) {
        this.vectorStore = vectorStore;
        this.cache = cache;
    }

    @Override
    public Map<String, Object> apply(DdslState state) {
        int retryCount = state.retrieverRetries();
        String userInput = state.userInput();

        log.info("RetrieverNode: querying knowledge base (attempt={})", retryCount);

        if (userInput == null || userInput.isBlank()) {
            return createErrorResult("Empty user input", retryCount);
        }

        String userInputHash = RetrievalCache.computeHash(userInput);

        Optional<List<Document>> cachedDocuments = cache.get(userInputHash);
        if (cachedDocuments.isPresent()) {
            log.info("Cache HIT for userInput hash: {}...", userInputHash.substring(0, Math.min(8, userInputHash.length())));
            return processDocuments(cachedDocuments.get(), retryCount, state);
        }

        log.info("Cache MISS for userInput hash: {}...", userInputHash.substring(0, Math.min(8, userInputHash.length())));

        try {
            // Retry strategy:
            // Retry 1: topK = 4 (default)
            // Retry 2: topK = 5 + loosen similarity threshold
            int adjustedTopK = retryCount > 0 ? 5 : 4;
            double adjustedThreshold = retryCount > 0 ? 0.65 : similarityThreshold;

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(userInput)
                    .topK(adjustedTopK)
                    .similarityThreshold(adjustedThreshold)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            cache.put(userInputHash, documents);

            return processDocuments(documents, retryCount, state);

        } catch (Exception e) {
            log.error("RetrieverNode: query failed", e);
            return createErrorResult(e.getMessage(), retryCount);
        }
    }

    private double calculateQualityScore(List<Document> documents) {
        if (documents.isEmpty()) {
            return 0.0;
        }
        
        double baseScore = Math.min(documents.size() / (double) topK, 1.0) * 0.7;
        
        int totalLength = documents.stream()
                .mapToInt(d -> d.getText().length())
                .sum();
        double lengthBonus = totalLength > 500 ? 0.15 : 0.0;
        
        long uniqueCategories = documents.stream()
                .map(d -> d.getMetadata().getOrDefault("category", "unknown"))
                .distinct()
                .count();
        double diversityBonus = uniqueCategories > 1 ? 0.15 : 0.0;
        
        return Math.min(baseScore + lengthBonus + diversityBonus, 1.0);
    }

    private Map<String, Object> processDocuments(List<Document> documents, int retryCount, DdslState state) {
        log.debug("Processing {} documents", documents.size());
        if (!documents.isEmpty()) {
            documents.forEach(doc -> log.debug("  Doc: id={}, text={}...",
                    doc.getId(), doc.getText().substring(0, Math.min(50, doc.getText().length()))));
        }

        double qualityScore = calculateQualityScore(documents);
        boolean qualityGood = qualityScore >= minQualityThreshold;

        StringBuilder contextBuilder = new StringBuilder();
        Set<String> categories = new HashSet<>();

        for (int i = 0; i < Math.min(documents.size(), topK); i++) {
            Document doc = documents.get(i);
            contextBuilder.append("## Context ").append(i + 1).append("\n");
            contextBuilder.append(doc.getText()).append("\n\n");

            String category = (String) doc.getMetadata().getOrDefault("category", "unknown");
            categories.add(category);
        }

        String retrievedContext = contextBuilder.toString().trim();

        log.info("RetrieverNode: processed {} docs, quality={:.2f}, good={}",
                documents.size(), qualityScore, qualityGood);

        Map<String, Object> updates = new HashMap<>();
        updates.put("retrievedContext", retrievedContext);
        updates.put("retrievalQuality", qualityScore);
        updates.put("retrieverRetries", retryCount + 1);

        if (!qualityGood && retryCount >= state.maxRetries()) {
            updates.put("lastError",
                    "Retrieval quality too low after " + retryCount + " retries");
            updates.put("errorStage", "RETRIEVER");
        }

        return updates;
    }

    private Map<String, Object> createErrorResult(String error, int retryCount) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("retrievedContext", "");
        updates.put("retrievalQuality", 0.0);
        updates.put("retrieverRetries", retryCount + 1);
        updates.put("lastError", error);
        updates.put("errorStage", "RETRIEVER");
        return updates;
    }
}
