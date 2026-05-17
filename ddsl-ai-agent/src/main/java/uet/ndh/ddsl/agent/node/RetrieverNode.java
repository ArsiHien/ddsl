package uet.ndh.ddsl.agent.node;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DdslState;
import uet.ndh.ddsl.agent.cache.RetrievalCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <b>Retriever Agent</b> — hybrid retrieval with quality assessment.
 * <p>
 * Generates query variants, queries Qdrant for dense semantic matches, scans the
 * classpath knowledge base for sparse lexical matches, merges candidates with
 * Reciprocal Rank Fusion, reranks by current task/mode, and emits compact fused
 * context for the Synthesizer.
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

    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String SPEC_RESOURCE_PATTERN = "classpath:dsl-spec/**/*.md";
    private static final int RRF_K = 60;
    private static final int MAX_QUERY_VARIANTS = 5;
    private static final int MAX_CONTEXT_CHARS = 3_500;
    private static final Pattern WORD_SPLIT = Pattern.compile("[^A-Za-z0-9_@<>!=]+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "by", "for", "from", "got", "in", "is", "it",
            "of", "on", "or", "that", "the", "this", "to", "with", "without"
    );

    private final VectorStore vectorStore;
    private final RetrievalCache cache;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private volatile List<Document> sparseCorpus;

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
        String query = state.retrievalQuery();
        if (query == null || query.isBlank()) {
            query = state.userInput();
        }

        log.info("RetrieverNode: querying knowledge base (attempt={})", retryCount);

        if (query == null || query.isBlank()) {
            return createErrorResult("Empty user input", retryCount, state);
        }

        List<String> queryVariants = rewriteQueries(query, state);
        String userInputHash = RetrievalCache.computeHash(String.join("\n---\n", queryVariants));

        Optional<List<Document>> cachedDocuments = cache.get(userInputHash);
        if (cachedDocuments.isPresent()) {
            log.info("Cache HIT for userInput hash: {}...", userInputHash.substring(0, Math.min(8, userInputHash.length())));
            return processDocuments(cachedDocuments.get(), retryCount, state, queryVariants);
        }

        log.info("Cache MISS for userInput hash: {}...", userInputHash.substring(0, Math.min(8, userInputHash.length())));

        try {
            int adjustedTopK = retryCount > 0 ? Math.max(topK + 2, 6) : Math.max(topK, 4);
            double adjustedThreshold = retryCount > 0 ? 0.65 : similarityThreshold;

            List<List<Document>> denseRankings = new ArrayList<>();
            for (String variant : queryVariants) {
                try {
                    SearchRequest searchRequest = SearchRequest.builder()
                            .query(variant)
                            .topK(adjustedTopK)
                            .similarityThreshold(adjustedThreshold)
                            .build();
                    denseRankings.add(vectorStore.similaritySearch(searchRequest));
                } catch (Exception e) {
                    log.warn("RetrieverNode: dense query variant failed: {}", e.getMessage());
                }
            }

            List<Document> sparseRanking = sparseSearch(queryVariants, adjustedTopK * 3);
            List<Document> documents = fuseAndRerank(denseRankings, sparseRanking, state)
                    .stream()
                    .limit(Math.max(topK, 4))
                    .toList();

            cache.put(userInputHash, documents);

            return processDocuments(documents, retryCount, state, queryVariants);

        } catch (Exception e) {
            log.error("RetrieverNode: query failed", e);
            return createErrorResult(e.getMessage(), retryCount, state);
        }
    }

    private List<String> rewriteQueries(String baseQuery, DdslState state) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(baseQuery.strip());

        String chunk = state.currentChunkId();
        String mode = state.synthesisMode();
        if (chunk != null && !chunk.isBlank()) {
            variants.add("DDSL " + chunk + " syntax examples " + baseQuery);
            variants.add(chunkConstructQuery(chunk));
        }

        String constructTerms = extractConstructTerms(baseQuery + " " + state.currentChunkTask());
        if (!constructTerms.isBlank()) {
            variants.add("DDSL keywords " + constructTerms + " parser compatible examples");
        }

        if ("REPAIR".equalsIgnoreCase(mode)) {
            String errorTerms = state.structuredErrors().stream()
                    .limit(5)
                    .map(error -> String.valueOf(error.getOrDefault("error_message", error.getOrDefault("message", ""))))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(" "));
            String logs = String.join(" ", state.errorLogs());
            variants.add("DDSL repair " + chunk + " " + errorTerms + " " + logs);
            variants.add("DDSL compiler error fix syntax " + firstWords(errorTerms + " " + logs, 24));
        }

        return variants.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .limit(MAX_QUERY_VARIANTS)
                .toList();
    }

    private String chunkConstructQuery(String chunk) {
        return switch (chunk) {
            case "DomainModel" -> "DDSL Aggregate Entity ValueObject operations require that then emit field identity syntax";
            case "DomainTypes" -> "DDSL ValueObject Enum field constraints identity required min max syntax";
            case "DomainServices" -> "DDSL DomainService dependency method service behavior syntax";
            case "DomainEvents" -> "DDSL DomainEvent event fields emitted from behavior syntax";
            case "Repositories" -> "DDSL Repository findBy save method syntax";
            case "Specifications" -> "DDSL Specification matches where and combines parser compatible syntax";
            default -> "DDSL " + chunk + " syntax parser compatible examples";
        };
    }

    private String extractConstructTerms(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> terms = new ArrayList<>();
        for (String term : List.of("aggregate", "entity", "valueobject", "domainservice", "domainevent",
                "repository", "specification", "factory", "operations", "invariants", "constraints",
                "behavior", "when", "require", "then", "emit", "identity", "required")) {
            if (lower.contains(term.toLowerCase(Locale.ROOT))) {
                terms.add(term);
            }
        }
        return String.join(" ", terms);
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

    private Map<String, Object> processDocuments(List<Document> documents, int retryCount, DdslState state, List<String> queryVariants) {
        log.debug("Processing {} documents", documents.size());
        if (!documents.isEmpty()) {
            documents.forEach(doc -> log.debug("  Doc: id={}, text={}...",
                    doc.getId(), doc.getText().substring(0, Math.min(50, doc.getText().length()))));
        }

        double qualityScore = calculateQualityScore(documents);
        boolean qualityGood = qualityScore >= minQualityThreshold;

        Set<String> categories = new HashSet<>();
        for (Document doc : documents) {
            String category = (String) doc.getMetadata().getOrDefault("category", "unknown");
            categories.add(category);
        }

        String retrievedContext = fuseContext(documents, state, queryVariants);
        String existingContext = baseContext(state.retrievedContext());
        if (existingContext != null && !existingContext.isBlank()) {
            retrievedContext = "## Required Context\n" + existingContext.strip()
                    + "\n\n" + retrievedContext;
        }

        log.info("RetrieverNode: processed {} docs, quality={}, good={}",
                documents.size(), String.format("%.2f", qualityScore), qualityGood);

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

    private List<Document> sparseSearch(List<String> queryVariants, int limit) {
        List<Document> corpus = sparseCorpus();
        if (corpus.isEmpty()) {
            return List.of();
        }

        Set<String> queryTokens = queryVariants.stream()
                .flatMap(q -> tokenize(q).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        Map<String, Long> documentFrequency = new HashMap<>();
        List<Set<String>> docTokenSets = new ArrayList<>();
        for (Document document : corpus) {
            Set<String> tokens = new HashSet<>(tokenize(searchableText(document)));
            docTokenSets.add(tokens);
            for (String token : tokens) {
                documentFrequency.merge(token, 1L, Long::sum);
            }
        }

        int corpusSize = Math.max(corpus.size(), 1);
        List<ScoredDocument> scored = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            Document document = corpus.get(i);
            List<String> docTokens = tokenize(searchableText(document));
            Map<String, Long> termFrequency = docTokens.stream()
                    .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

            double score = 0.0;
            for (String token : queryTokens) {
                long tf = termFrequency.getOrDefault(token, 0L);
                if (tf == 0) {
                    continue;
                }
                long df = documentFrequency.getOrDefault(token, 1L);
                double idf = Math.log(1.0 + (corpusSize - df + 0.5) / (df + 0.5));
                score += (1.0 + Math.log(tf)) * idf;
            }

            if (score > 0.0) {
                scored.add(new ScoredDocument(document, score));
            }
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .limit(limit)
                .map(ScoredDocument::document)
                .toList();
    }

    private List<Document> fuseAndRerank(List<List<Document>> denseRankings, List<Document> sparseRanking, DdslState state) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();

        for (List<Document> ranking : denseRankings) {
            addRanking(candidates, ranking, "dense", 1.0);
        }
        addRanking(candidates, sparseRanking, "sparse", 1.2);

        return candidates.values().stream()
                .peek(candidate -> candidate.score += rerankBonus(candidate.document, state, candidate.channels))
                .sorted(Comparator.comparingDouble((Candidate c) -> c.score).reversed())
                .map(candidate -> candidate.document)
                .toList();
    }

    private void addRanking(Map<String, Candidate> candidates, List<Document> ranking, String channel, double weight) {
        for (int i = 0; i < ranking.size(); i++) {
            Document document = ranking.get(i);
            String key = documentKey(document);
            Candidate candidate = candidates.computeIfAbsent(key, ignored -> new Candidate(document));
            candidate.score += weight / (RRF_K + i + 1);
            candidate.channels.add(channel);
        }
    }

    private double rerankBonus(Document document, DdslState state, Set<String> channels) {
        String text = searchableText(document).toLowerCase(Locale.ROOT);
        double bonus = 0.0;

        if (channels.contains("dense") && channels.contains("sparse")) {
            bonus += 0.05;
        }
        String chunk = state.currentChunkId();
        if (chunk != null && !chunk.isBlank() && text.contains(chunk.toLowerCase(Locale.ROOT))) {
            bonus += 0.04;
        }
        for (String token : tokenize(chunkConstructQuery(chunk))) {
            if (text.contains(token)) {
                bonus += 0.005;
            }
        }
        if (containsDdslCode(text)) {
            bonus += 0.04;
        } else {
            bonus -= 0.02;
        }
        if ("REPAIR".equalsIgnoreCase(state.synthesisMode())) {
            String errors = (state.compilerFeedback() + " " + String.join(" ", state.errorLogs())).toLowerCase(Locale.ROOT);
            for (String token : tokenize(errors)) {
                if (text.contains(token)) {
                    bonus += 0.004;
                }
            }
            if (text.contains("repair") || text.contains("error") || text.contains("fix")) {
                bonus += 0.03;
            }
        }
        return bonus;
    }

    private String fuseContext(List<Document> documents, DdslState state, List<String> queryVariants) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Retrieval Summary\n");
        sb.append("- mode: ").append(state.synthesisMode()).append("\n");
        sb.append("- chunk: ").append(blankToDash(state.currentChunkId())).append("\n");
        sb.append("- query variants: ").append(String.join(" | ", queryVariants)).append("\n");
        sb.append("- fused candidates: ").append(documents.size()).append("\n\n");

        sb.append("## Required DDSL Syntax\n");
        appendSectionLines(sb, documents, true, 12);

        sb.append("\n## Relevant Examples\n");
        appendSectionLines(sb, documents, false, 18);

        sb.append("\n## Repair Hints\n");
        if ("REPAIR".equalsIgnoreCase(state.synthesisMode())) {
            state.errorLogs().stream().limit(4).forEach(error -> sb.append("- ").append(error).append("\n"));
            state.structuredErrors().stream().limit(4)
                    .map(error -> String.valueOf(error.getOrDefault("fix_hint", error.getOrDefault("suggestion", ""))))
                    .filter(s -> !s.isBlank() && !"null".equals(s))
                    .forEach(hint -> sb.append("- ").append(hint).append("\n"));
        } else {
            sb.append("- Prefer exact parser-compatible DDSL syntax from the examples above.\n");
            sb.append("- Keep generated chunk scoped to the current chunk task and dependencies.\n");
        }

        String context = sb.toString().strip();
        return context.length() <= MAX_CONTEXT_CHARS ? context : context.substring(0, MAX_CONTEXT_CHARS).strip();
    }

    private void appendSectionLines(StringBuilder sb, List<Document> documents, boolean syntaxOnly, int maxLines) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (Document document : documents) {
            for (String line : document.getText().split("\\R")) {
                String trimmed = line.strip();
                if (trimmed.isBlank() || trimmed.equals("---")) {
                    continue;
                }
                boolean syntaxLine = isSyntaxLine(trimmed);
                if ((syntaxOnly && syntaxLine) || (!syntaxOnly && (syntaxLine || containsDdslCode(trimmed)))) {
                    lines.add("- " + trimTo(trimmed, 180));
                }
                if (lines.size() >= maxLines) {
                    break;
                }
            }
            if (lines.size() >= maxLines) {
                break;
            }
        }
        if (lines.isEmpty()) {
            sb.append("- No direct syntax snippet found; use retrieved summary only.\n");
            return;
        }
        lines.forEach(line -> sb.append(line).append("\n"));
    }

    private boolean isSyntaxLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("boundedcontext") || lower.contains("aggregate ") || lower.contains("entity ")
                || lower.contains("valueobject") || lower.contains("domainservice") || lower.contains("domainevent")
                || lower.contains("repository ") || lower.contains("specification ") || lower.contains("when ")
                || lower.contains("require that") || lower.contains("then:") || lower.contains("emit ")
                || lower.contains("@identity") || lower.contains("@required") || lower.contains("matches ");
    }

    private List<Document> sparseCorpus() {
        List<Document> current = sparseCorpus;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (sparseCorpus != null) {
                return sparseCorpus;
            }
            sparseCorpus = loadSparseCorpus();
            return sparseCorpus;
        }
    }

    private List<Document> loadSparseCorpus() {
        try {
            Resource[] resources = resolver.getResources(SPEC_RESOURCE_PATTERN);
            List<Document> documents = new ArrayList<>();
            for (Resource resource : resources) {
                parseMarkdownDocument(resource).ifPresent(documents::add);
            }
            log.info("RetrieverNode: loaded {} local sparse docs", documents.size());
            return List.copyOf(documents);
        } catch (IOException e) {
            log.warn("RetrieverNode: failed to load local sparse corpus: {}", e.getMessage());
            return List.of();
        }
    }

    private Optional<Document> parseMarkdownDocument(Resource resource) {
        try {
            String raw = resource.getContentAsString(StandardCharsets.UTF_8).strip();
            if (!raw.startsWith(FRONT_MATTER_DELIMITER)) {
                return Optional.empty();
            }

            int secondDelimiter = raw.indexOf(FRONT_MATTER_DELIMITER, FRONT_MATTER_DELIMITER.length());
            if (secondDelimiter < 0) {
                return Optional.empty();
            }

            String yamlBlock = raw.substring(FRONT_MATTER_DELIMITER.length(), secondDelimiter).strip();
            String content = raw.substring(secondDelimiter + FRONT_MATTER_DELIMITER.length()).strip();
            if (content.isBlank()) {
                return Optional.empty();
            }

            Map<String, Object> metadata = parseSimpleFrontMatter(yamlBlock);
            String rawId = String.valueOf(metadata.getOrDefault("id", resource.getFilename()));
            metadata.put("original_id", rawId);
            metadata.put("source", "local-ddsl-knowledge-base");
            metadata.put("file", resource.getFilename());
            String id = UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)).toString();
            return Optional.of(new Document(id, content, metadata));
        } catch (Exception e) {
            log.debug("RetrieverNode: failed to parse sparse doc {}: {}", resource.getFilename(), e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> parseSimpleFrontMatter(String yamlBlock) {
        Map<String, Object> metadata = new HashMap<>();
        for (String line : yamlBlock.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).strip();
            String value = line.substring(colon + 1).strip();
            if (!key.isBlank()) {
                metadata.put(key, value);
            }
        }
        return metadata;
    }

    private String searchableText(Document document) {
        return document.getText() + " " + document.getMetadata().values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(WORD_SPLIT.split(text.toLowerCase(Locale.ROOT)))
                .filter(token -> token.length() > 1)
                .filter(token -> !STOPWORDS.contains(token))
                .toList();
    }

    private String firstWords(String text, int maxWords) {
        return tokenize(text).stream().limit(maxWords).collect(Collectors.joining(" "));
    }

    private boolean containsDdslCode(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("boundedcontext") || lower.contains("aggregate ") || lower.contains("entity ")
                || lower.contains("valueobject") || lower.contains("domainservice") || lower.contains("domainevent")
                || lower.contains("repository ") || lower.contains("specification ") || lower.contains("when ");
    }

    private String documentKey(Document document) {
        if (document.getId() != null && !document.getId().isBlank()) {
            return document.getId();
        }
        return RetrievalCache.computeHash(document.getText());
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String trimTo(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3).strip() + "...";
    }

    private String baseContext(String context) {
        if (context == null || context.isBlank()) {
            return "";
        }
        String marker = context.contains("## Retrieval Summary")
                ? "## Retrieval Summary"
                : "## Retrieved Similar Examples";
        int markerIndex = context.indexOf(marker);
        if (context.startsWith("## Required Context") && markerIndex > 0) {
            return context.substring("## Required Context".length(), markerIndex).strip();
        }
        return context;
    }

    private Map<String, Object> createErrorResult(String error, int retryCount, DdslState state) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("retrievedContext", baseContext(state.retrievedContext()));
        updates.put("retrievalQuality", 0.0);
        updates.put("retrieverRetries", retryCount + 1);
        updates.put("lastError", error);
        updates.put("errorStage", "RETRIEVER");
        return updates;
    }

    private record ScoredDocument(Document document, double score) {
    }

    private static final class Candidate {
        private final Document document;
        private final Set<String> channels = new HashSet<>();
        private double score;

        private Candidate(Document document) {
            this.document = document;
        }
    }
}
