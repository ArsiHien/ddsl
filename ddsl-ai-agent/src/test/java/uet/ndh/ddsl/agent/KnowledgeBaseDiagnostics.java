package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Diagnostic test to verify Qdrant knowledge base retrieval.
 * Run this to check if documents are properly embedded and searchable.
 */
@SpringBootTest
@Slf4j
public class KnowledgeBaseDiagnostics {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void checkKnowledgeBaseContents() {
        // Test queries that should match documentation
        String[] testQueries = {
            "Create an Order aggregate",
            "when placing order with customer",
            "orderId UUID identity",
            "field declarations",
            "behavior patterns",
            "aggregate syntax"
        };

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Knowledge Base Diagnostics");
        System.out.println("=".repeat(80));

        for (String query : testQueries) {
            System.out.println("\nQuery: " + query);
            System.out.println("-".repeat(80));

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(4)
                    .similarityThreshold(0.0)  // No threshold to see all results
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            System.out.println("Found " + documents.size() + " documents");

            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                String category = (String) doc.getMetadata().getOrDefault("category", "unknown");
                String subcategory = (String) doc.getMetadata().getOrDefault("subcategory", "unknown");
                String file = (String) doc.getMetadata().getOrDefault("file", "unknown");
                Number score = (Number) doc.getMetadata().getOrDefault("distance", 0.0);

                System.out.printf("  %d. [%s] %s (score: %.4f)%n", i + 1, category, file, score.doubleValue());
                System.out.printf("     Subcategory: %s%n", subcategory);
                String preview = doc.getText().substring(0, Math.min(100, doc.getText().length()));
                System.out.printf("     Preview: %s...%n%n", preview.replace("\n", " "));
            }
        }

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    void testSpecificNlToDslQueries() {
        // These are the exact queries from the mini test suite
        String[] testCases = {
            "Create an Order aggregate with orderId as UUID and identity, customerName as text and required",
            "Order aggregate with orderId UUID identity, customerName text required, items list of OrderItem",
            "Order aggregate with orderId UUID identity, customerName text required, status text default PENDING"
        };

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Testing Mini Test Suite Queries");
        System.out.println("=".repeat(80));

        for (String query : testCases) {
            System.out.println("\nQuery: " + query.substring(0, Math.min(80, query.length())) + "...");

            // Test with high threshold (0.75)
            SearchRequest highThresholdRequest = SearchRequest.builder()
                    .query(query)
                    .topK(4)
                    .similarityThreshold(0.75)
                    .build();

            List<Document> highResults = vectorStore.similaritySearch(highThresholdRequest);
            System.out.println("  With threshold 0.75: " + highResults.size() + " docs");

            // Test with low threshold (0.0)
            SearchRequest lowThresholdRequest = SearchRequest.builder()
                    .query(query)
                    .topK(4)
                    .similarityThreshold(0.0)
                    .build();

            List<Document> lowResults = vectorStore.similaritySearch(lowThresholdRequest);
            System.out.println("  With threshold 0.00: " + lowResults.size() + " docs");

            if (!lowResults.isEmpty()) {
                Document best = lowResults.get(0);
                Number score = (Number) best.getMetadata().getOrDefault("distance", 0.0);
                String file = (String) best.getMetadata().getOrDefault("file", "unknown");
                System.out.println("  Best match: " + file + " (score: " + String.format("%.4f", score.doubleValue()) + ")");
            }
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }
}
