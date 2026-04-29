package uet.ndh.ddsl.agent.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Caffeine-based cache for Qdrant retrieval results.
 * <p>
 * Cache key is SHA-256 hash of userInput (truncated to 32 chars).
 * Configuration: 500 max entries, 30-minute TTL.
 * <p>
 * Edge case: if cached List<Document> is empty, treat as cache miss (re-fetch from Qdrant).
 */
@Slf4j
public class RetrievalCache {

    private final Cache<String, List<Document>> cache;

    public RetrievalCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();
    }

    /**
     * Retrieves cached documents for the given user input hash.
     *
     * @param userInputHash SHA-256 hash of userInput (32 chars)
     * @return Optional containing documents if cache hit and non-empty, empty otherwise
     */
    public Optional<List<Document>> get(String userInputHash) {
        List<Document> documents = cache.getIfPresent(userInputHash);

        if (documents == null) {
            log.debug("Cache MISS for key: {}...", userInputHash.substring(0, Math.min(8, userInputHash.length())));
            return Optional.empty();
        }

        // Edge case: empty list cached means cache miss (trigger re-fetch)
        if (documents.isEmpty()) {
            log.debug("Cache HIT but empty list for key: {}... — treating as cache miss",
                    userInputHash.substring(0, Math.min(8, userInputHash.length())));
            cache.invalidate(userInputHash);
            return Optional.empty();
        }

        log.debug("Cache HIT for key: {}... ({} documents)",
                userInputHash.substring(0, Math.min(8, userInputHash.length())),
                documents.size());
        return Optional.of(documents);
    }

    /**
     * Stores documents in cache for the given user input hash.
     *
     * @param userInputHash SHA-256 hash of userInput (32 chars)
     * @param documents     List<Document> to cache
     */
    public void put(String userInputHash, List<Document> documents) {
        // Do NOT cache null or empty lists — treat as cache miss
        if (documents == null || documents.isEmpty()) {
            log.debug("Skipping cache put for null/empty documents, key: {}...",
                    userInputHash.substring(0, Math.min(8, userInputHash.length())));
            return;
        }

        cache.put(userInputHash, documents);
        log.debug("Cached {} documents for key: {}...",
                documents.size(),
                userInputHash.substring(0, Math.min(8, userInputHash.length())));
    }

    /**
     * Invalidates cache entry for the given user input hash.
     *
     * @param userInputHash SHA-256 hash of userInput (32 chars)
     */
    public void invalidate(String userInputHash) {
        cache.invalidate(userInputHash);
        log.debug("Cache invalidated for key: {}...",
                userInputHash.substring(0, Math.min(8, userInputHash.length())));
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        cache.invalidateAll();
        log.debug("Cache cleared");
    }

    /**
     * Returns current cache size (number of entries).
     *
     * @return number of entries in cache
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * Computes SHA-256 hash of input string, truncated to 32 characters.
     *
     * @param input the input string to hash
     * @return SHA-256 hash truncated to 32 chars
     */
    public static String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in Java
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}