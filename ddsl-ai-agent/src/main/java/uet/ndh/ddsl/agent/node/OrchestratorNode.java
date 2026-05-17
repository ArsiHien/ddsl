package uet.ndh.ddsl.agent.node;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DdslState;
import uet.ndh.ddsl.agent.dto.PlanStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Central planner for modular NL -> DDSL generation.
 * <p>
 * The orchestrator owns the chunk plan, dispatches retrieval/synthesis work,
 * merges chunk outputs into a single DSL file, and scopes repair attempts to
 * the chunk reported by the Judge.
 */
@Component
@Slf4j
public class OrchestratorNode implements NodeAction<DdslState> {

    public static final String ROUTE_RETRIEVER = "retriever";
    public static final String ROUTE_SYNTHESIZER = "synthesizer";
    public static final String ROUTE_JUDGE = "judge";
    public static final String ROUTE_END = "end";

    private static final String PHASE_RETRIEVING = "RETRIEVING";
    private static final String PHASE_SYNTHESIZING = "SYNTHESIZING";
    private static final String PHASE_JUDGING = "JUDGING";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REPAIRING = "REPAIRING";
    private static final String STATUS_FAILED = "FAILED";

    private static final int MAX_REPAIR_ATTEMPTS_PER_CHUNK = 3;

    @Override
    public Map<String, Object> apply(DdslState state) {
        if (state.planGraph().isEmpty()) {
            log.info("OrchestratorNode: creating modular plan graph");
            return startPlan(state);
        }

        return switch (state.orchestratorPhase()) {
            case PHASE_RETRIEVING -> dispatchSynthesis(state);
            case PHASE_SYNTHESIZING -> mergeAndContinue(state);
            case PHASE_JUDGING -> handleJudgeResult(state);
            default -> routeEnd();
        };
    }

    private Map<String, Object> startPlan(DdslState state) {
        List<PlanStep> plan = buildPlan(state.userInput());

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan));
        selectChunk(updates, plan, plan.getFirst().id(), state, "GENERATE");
        return updates;
    }

    private List<PlanStep> buildPlan(String userInput) {
        String normalized = userInput == null ? "" : userInput.toLowerCase(Locale.ROOT);
        boolean includeUbiquitousLanguage = mentionsAny(normalized,
                "ubiquitous language", "glossary", "terms", "definition", "vocabulary");
        boolean includeDomainTypes = mentionsAny(normalized,
                "valueobject", "value object", "vo ", "money", "address", "contact info",
                "date range", "daterange", "email value", "custom type", "enum");
        boolean includeStateMachines = mentionsAny(normalized,
                "state machine", "statemachine", "state transition", "transition", "lifecycle");
        boolean includeDomainServices = mentionsAny(normalized,
                "domainservice", "domain service", "service", "policy", "workflow",
                "calculate", "pricing", "availability", "eligibility", "validate");
        boolean includeEvents = mentionsAny(normalized, "domainevent", "domain event", "event", "emit", "publish");
        boolean includeEventHandlers = mentionsAny(normalized, "event handler", "eventhandler", "handler", "listen", "subscribe");
        boolean includeFactories = mentionsAny(normalized, "factory", "create from", "creating");
        boolean includeRepositories = mentionsAny(normalized, "repository", "repositories", "repo", "persist", "save", "find by", "query");
        boolean includeSpecifications = mentionsAny(normalized, "specification", "specifications", "criteria", "eligible", "matches");
        boolean includeUseCases = mentionsAny(normalized, "use case", "usecase", "use-cases", "application service", "applicationservice");

        List<PlanStep> plan = new ArrayList<>();
        if (includeUbiquitousLanguage) {
            plan.add(step("UbiquitousLanguage",
                    "Define only explicitly requested ubiquitous-language terms and definitions. Output raw term definitions only, without BoundedContext or ubiquitous-language wrapper.",
                    List.of()));
        }
        if (includeDomainTypes) {
            plan.add(step("DomainTypes",
                    "Define only explicitly requested Enum and ValueObject declarations. Do not define aggregates, entities, services, events, repositories, or @identity fields. ValueObjects never use @identity.",
                    List.of()));
        }

        List<String> modelDependencies = includeDomainTypes ? List.of("DomainTypes") : List.of();
        plan.add(step("DomainModel",
                "Define Aggregate and Entity declarations with fields, identities, invariants, and aggregate/entity operations. Do not define ValueObjects unless explicitly requested in DomainTypes. Every Aggregate/Entity identity uses @identity on the aggregate/entity field only.",
                modelDependencies));

        List<String> downstreamDependencies = includeDomainTypes ? List.of("DomainTypes", "DomainModel") : List.of("DomainModel");
        if (includeStateMachines) {
            plan.add(step("StateMachines",
                    "Define only explicitly requested StateMachine declarations for aggregate/entity lifecycle transitions. Output raw StateMachine declarations only.",
                    downstreamDependencies));
        }
        if (includeDomainServices) {
            plan.add(step("DomainServices",
                    "Define only explicitly requested DomainService declarations and behavior workflows. If no service/policy/workflow is requested, output nothing.",
                    downstreamDependencies));
        }
        if (includeEvents) {
            plan.add(step("DomainEvents",
                    "Define only explicitly requested DomainEvent declarations. Output raw DomainEvent declarations only.",
                    downstreamDependencies));
        }
        if (includeEventHandlers) {
            List<String> deps = includeEvents ? addDependency(downstreamDependencies, "DomainEvents") : downstreamDependencies;
            plan.add(step("EventHandlers",
                    "Define only explicitly requested EventHandler declarations. Output raw EventHandler declarations only.",
                    deps));
        }
        if (includeFactories) {
            plan.add(step("Factories",
                    "Define only explicitly requested Factory declarations. Output raw Factory declarations only.",
                    downstreamDependencies));
        }
        if (includeRepositories) {
            plan.add(step("Repositories",
                    "Define only explicitly requested Repository declarations. Output raw Repository declarations only.",
                    downstreamDependencies));
        }
        if (includeSpecifications) {
            plan.add(step("Specifications",
                    "Define only explicitly requested Specification declarations. Output raw Specification declarations only.",
                    downstreamDependencies));
        }
        if (includeUseCases) {
            List<String> deps = new ArrayList<>(downstreamDependencies);
            if (includeRepositories) deps.add("Repositories");
            if (includeDomainServices) deps.add("DomainServices");
            if (includeFactories) deps.add("Factories");
            plan.add(step("UseCases",
                    "Define only explicitly requested UseCase declarations for the use-cases section. Output raw UseCase declarations only.",
                    deps));
        }

        log.info("OrchestratorNode: plan chunks={}", plan.stream().map(PlanStep::id).toList());
        return plan;
    }

    private List<String> addDependency(List<String> dependencies, String dependency) {
        List<String> copy = new ArrayList<>(dependencies);
        copy.add(dependency);
        return copy;
    }

    private boolean mentionsAny(String input, String... terms) {
        for (String term : terms) {
            if (input.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> dispatchSynthesis(DdslState state) {
        String chunkId = state.currentChunkId();
        Map<String, String> contexts = new LinkedHashMap<>(state.chunkContexts());
        contexts.put(chunkId, state.retrievedContext());

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(DdslState.KEY_CHUNK_CONTEXTS, contexts);
        updates.put(DdslState.KEY_ORCHESTRATOR_PHASE, PHASE_SYNTHESIZING);
        updates.put(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_SYNTHESIZER);
        return updates;
    }

    private Map<String, Object> mergeAndContinue(DdslState state) {
        String chunkId = state.currentChunkId();
        Map<String, String> outputs = new LinkedHashMap<>(state.chunkOutputs());
        String previousChunkCode = outputs.getOrDefault(chunkId, "");
        String generatedChunkCode = state.currentDsl();
        String chunkCode = generatedChunkCode;
        boolean emptyRepairOutput = "REPAIR".equalsIgnoreCase(state.synthesisMode())
                && (generatedChunkCode == null || generatedChunkCode.isBlank())
                && previousChunkCode != null && !previousChunkCode.isBlank();
        if (emptyRepairOutput) {
            log.warn("OrchestratorNode: ignoring empty repair output for {}; keeping previous chunk", chunkId);
            chunkCode = previousChunkCode;
        }
        outputs.put(chunkId, chunkCode);

        MergeResult merged = merge(state.userInput(), outputs);
        List<PlanStep> plan = PlanStep.fromMaps(state.planGraph());

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(DdslState.KEY_CHUNK_OUTPUTS, outputs);
        updates.put(DdslState.KEY_CURRENT_CHUNK_CODE, chunkCode);
        updates.put(DdslState.KEY_CURRENT_DSL, merged.dsl());
        updates.put(DdslState.KEY_CHUNK_LINE_MAP, merged.lineMap());
        updates.put(DdslState.KEY_IS_SUCCESSFUL, false);

        if ("REPAIR".equalsIgnoreCase(state.synthesisMode())) {
            updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan));
            updates.put(DdslState.KEY_ORCHESTRATOR_PHASE, PHASE_JUDGING);
            updates.put(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_JUDGE);
            return updates;
        }

        plan = updateStatus(plan, chunkId, STATUS_GENERATED);
        Optional<String> nextChunk = nextRunnable(plan);
        if (nextChunk.isPresent()) {
            updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan));
            selectChunk(updates, plan, nextChunk.get(), state, "GENERATE", outputs);
            return updates;
        }

        log.info("OrchestratorNode: all chunks generated, merging final DSL and calling Judge once");
        updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan));
        updates.put(DdslState.KEY_ORCHESTRATOR_PHASE, PHASE_JUDGING);
        updates.put(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_JUDGE);
        return updates;
    }

    private Map<String, Object> handleJudgeResult(DdslState state) {
        if (state.isSuccessful()) {
            return finishSuccess(state);
        }
        return scheduleRepair(state);
    }

    private Map<String, Object> finishSuccess(DdslState state) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(approveGeneratedChunks(PlanStep.fromMaps(state.planGraph()))));
        updates.put(DdslState.KEY_REPAIR_HISTORY, List.<Map<String, Object>>of());
        updates.put(DdslState.KEY_IS_SUCCESSFUL, true);
        updates.put(DdslState.KEY_FINAL_DSL, state.currentDsl());
        updates.put(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_END);
        updates.put(DdslState.KEY_ORCHESTRATOR_PHASE, "DONE");
        return updates;
    }

    private Map<String, Object> scheduleRepair(DdslState state) {
        String failedChunk = inferFailedChunk(state).orElse(state.currentChunkId());
        int repairCount = countRepairs(state.repairHistory(), failedChunk);

        Map<String, Object> repairEntry = new LinkedHashMap<>();
        repairEntry.put("chunkId", failedChunk);
        repairEntry.put("attempt", repairCount + 1);
        repairEntry.put("errors", state.structuredErrors());

        List<Map<String, Object>> repairHistory = new ArrayList<>(state.repairHistory());
        repairHistory.add(repairEntry);

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(DdslState.KEY_REPAIR_HISTORY, repairHistory);

        if (repairCount >= MAX_REPAIR_ATTEMPTS_PER_CHUNK) {
            log.warn("OrchestratorNode: repair limit reached for {}", failedChunk);
            updates.put(DdslState.KEY_PLAN_GRAPH,
                    PlanStep.toMaps(updateStatus(PlanStep.fromMaps(state.planGraph()), failedChunk, STATUS_FAILED)));
            updates.put(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_END);
            updates.put(DdslState.KEY_ORCHESTRATOR_PHASE, "FAILED");
            return updates;
        }

        List<PlanStep> plan = updateStatus(PlanStep.fromMaps(state.planGraph()), failedChunk, STATUS_REPAIRING);
        updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan));
        selectChunk(updates, plan, failedChunk, state, "REPAIR");
        return updates;
    }

    private void selectChunk(Map<String, Object> updates, List<PlanStep> plan, String chunkId, DdslState state, String mode) {
        selectChunk(updates, plan, chunkId, state, mode, state.chunkOutputs());
    }

    private void selectChunk(
            Map<String, Object> updates,
            List<PlanStep> plan,
            String chunkId,
            DdslState state,
            String mode,
            Map<String, String> outputs
    ) {
        PlanStep chunk = findChunk(plan, chunkId).orElseThrow();
        String task = chunk.task();
        String contextBlock = buildDependencyContext(chunk, outputs);
        String retrievalQuery = buildRetrievalQuery(task, mode, state);

        updates.put(DdslState.KEY_CURRENT_CHUNK_ID, chunkId);
        updates.put(DdslState.KEY_CURRENT_CHUNK_TASK, task);
        updates.put(DdslState.KEY_CURRENT_CHUNK_CODE, outputs.getOrDefault(chunkId, ""));
        updates.put(DdslState.KEY_RETRIEVAL_QUERY, retrievalQuery);
        updates.put(DdslState.KEY_RETRIEVED_CONTEXT, contextBlock);
        updates.put(DdslState.KEY_SYNTHESIS_MODE, mode);
        updates.put(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(updateStatus(plan, chunkId, STATUS_RUNNING)));
        updates.put(DdslState.KEY_RETRIEVER_RETRIES, 0);
        updates.put(DdslState.KEY_SYNTHESIZER_RETRIES, 0);
        updates.put(DdslState.KEY_ORCHESTRATOR_PHASE, PHASE_RETRIEVING);
        updates.put(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_RETRIEVER);
    }

    private String buildDependencyContext(PlanStep chunk, Map<String, String> outputs) {
        List<String> dependencies = chunk.dependsOn();
        StringBuilder sb = new StringBuilder();
        for (String dependency : dependencies) {
            String code = outputs.get(dependency);
            if (code != null && !code.isBlank()) {
                sb.append("## Stub from ").append(dependency).append("\n");
                sb.append(toStub(code)).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    private String toStub(String code) {
        StringBuilder sb = new StringBuilder();
        for (String line : code.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.matches("^(Aggregate|Entity|Enum|ValueObject|DomainService|StateMachine|DomainEvent|EventHandler|Repository|Factory|Specification|UseCase)\\b.*")
                    || trimmed.matches("^[A-Za-z][A-Za-z0-9_]*\\s*:\\s*[^{}]+$")) {
                sb.append(trimmed).append("\n");
            }
        }
        return sb.isEmpty() ? code : sb.toString().trim();
    }

    private String buildRetrievalQuery(String task, String mode, DdslState state) {
        if ("REPAIR".equals(mode)) {
            return "DDSL repair examples for " + state.currentChunkId() + "\n"
                    + state.compilerFeedback() + "\n"
                    + String.join("\n", state.errorLogs());
        }
        return task + "\n\nIntent:\n" + state.userInput();
    }

    private MergeResult merge(String userInput, Map<String, String> outputs) {
        String boundedContextName = inferBoundedContextName(userInput);
        StringBuilder sb = new StringBuilder();
        Map<String, Map<String, Integer>> lineMap = new LinkedHashMap<>();

        appendLine(sb, "BoundedContext " + boundedContextName + " {");
        appendLine(sb, "");
        appendWrappedChunk(sb, lineMap, "UbiquitousLanguage", "ubiquitous-language", outputs.get("UbiquitousLanguage"), 4, 8);
        appendLine(sb, "    domain {");
        appendChunk(sb, lineMap, "DomainTypes", outputs.get("DomainTypes"), 8);
        appendChunk(sb, lineMap, "DomainModel", outputs.get("DomainModel"), 8);
        appendChunk(sb, lineMap, "StateMachines", outputs.get("StateMachines"), 8);
        appendChunk(sb, lineMap, "DomainServices", outputs.get("DomainServices"), 8);
        appendLine(sb, "    }");
        appendLine(sb, "");
        appendWrappedChunk(sb, lineMap, "DomainEvents", "events", outputs.get("DomainEvents"), 4, 8);
        appendWrappedChunk(sb, lineMap, "EventHandlers", "event-handlers", outputs.get("EventHandlers"), 4, 8);
        appendWrappedChunk(sb, lineMap, "Factories", "factories", outputs.get("Factories"), 4, 8);
        appendWrappedChunk(sb, lineMap, "Repositories", "repositories", outputs.get("Repositories"), 4, 8);
        appendWrappedChunk(sb, lineMap, "Specifications", "specifications", outputs.get("Specifications"), 4, 8);
        appendWrappedChunk(sb, lineMap, "UseCases", "use-cases", outputs.get("UseCases"), 4, 8);
        appendLine(sb, "}");
        return new MergeResult(sb.toString(), lineMap);
    }

    private void appendWrappedChunk(
            StringBuilder sb,
            Map<String, Map<String, Integer>> lineMap,
            String chunkId,
            String sectionName,
            String code,
            int sectionIndent,
            int declarationIndent
    ) {
        if (code == null || code.isBlank()) {
            return;
        }

        String sanitized = sanitizeChunk(code);
        if (sanitized.isBlank()) {
            return;
        }

        String sectionPadding = " ".repeat(Math.max(0, sectionIndent));
        String declarationPadding = " ".repeat(Math.max(0, declarationIndent));
        appendLine(sb, sectionPadding + sectionName + " {");
        appendLine(sb, declarationPadding + "// @chunk:start " + chunkId);
        int start = lineCount(sb) + 1;
        for (String line : sanitized.split("\\R")) {
            appendLine(sb, declarationPadding + line);
        }
        int end = lineCount(sb);
        appendLine(sb, declarationPadding + "// @chunk:end " + chunkId);
        appendLine(sb, sectionPadding + "}");
        appendLine(sb, "");
        lineMap.put(chunkId, Map.of("start", start, "end", end));
    }

    private void appendChunk(StringBuilder sb, Map<String, Map<String, Integer>> lineMap, String chunkId, String code, int indent) {
        if (code == null || code.isBlank()) {
            return;
        }

        appendLine(sb, " ".repeat(Math.max(0, indent)) + "// @chunk:start " + chunkId);
        int start = lineCount(sb) + 1;
        for (String line : sanitizeChunk(code).split("\\R")) {
            appendLine(sb, " ".repeat(Math.max(0, indent)) + line);
        }
        int end = lineCount(sb);
        appendLine(sb, " ".repeat(Math.max(0, indent)) + "// @chunk:end " + chunkId);
        lineMap.put(chunkId, Map.of("start", start, "end", end));
    }

    private String sanitizeChunk(String code) {
        String sanitized = code.strip();
        sanitized = stripSingleWrapper(sanitized, "BoundedContext\\s+\\w+");
        sanitized = stripSingleWrapper(sanitized, "(?:domain|events|event-handlers|repositories|factories|specifications|use-cases|ubiquitous-language)");
        return sanitized.strip();
    }

    private String stripSingleWrapper(String code, String wrapperPattern) {
        Pattern pattern = Pattern.compile("(?s)^\\s*" + wrapperPattern + "\\s*\\{(.*)}\\s*$");
        Matcher matcher = pattern.matcher(code);
        if (matcher.matches()) {
            return matcher.group(1).strip();
        }
        return code;
    }

    private void appendLine(StringBuilder sb, String line) {
        sb.append(line).append("\n");
    }

    private int lineCount(StringBuilder sb) {
        return (int) sb.toString().lines().count();
    }

    private Optional<String> inferFailedChunk(DdslState state) {
        for (Map<String, Object> error : state.structuredErrors()) {
            Object sourceChunk = error.get("source_chunk");
            if (sourceChunk instanceof String s && !s.isBlank()) {
                return Optional.of(s);
            }
            Object lineNumber = error.get("line_number");
            if (lineNumber instanceof Number n) {
                Optional<String> chunk = chunkForLine(state.chunkLineMap(), n.intValue());
                if (chunk.isPresent()) {
                    return chunk;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> chunkForLine(Map<String, Map<String, Integer>> lineMap, int line) {
        return lineMap.entrySet().stream()
                .filter(e -> line >= e.getValue().getOrDefault("start", -1)
                        && line <= e.getValue().getOrDefault("end", -1))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private int countRepairs(List<Map<String, Object>> history, String chunkId) {
        return (int) history.stream()
                .filter(entry -> chunkId.equals(entry.get("chunkId")))
                .count();
    }

    private Optional<String> nextRunnable(List<PlanStep> plan) {
        for (PlanStep chunk : plan) {
            String status = chunk.status();
            if (!STATUS_PENDING.equals(status)) {
                continue;
            }
            List<String> dependencies = chunk.dependsOn();
            boolean dependenciesApproved = dependencies.stream()
                    .allMatch(dep -> findChunk(plan, dep)
                            .map(c -> isDependencyReady(c.status()))
                            .orElse(false));
            if (dependenciesApproved) {
                return Optional.of(chunk.id());
            }
        }
        return Optional.empty();
    }

    private List<PlanStep> updateStatus(List<PlanStep> plan, String chunkId, String status) {
        List<PlanStep> updated = new ArrayList<>();
        for (PlanStep chunk : plan) {
            updated.add(chunkId.equals(chunk.id()) ? chunk.withStatus(status) : chunk);
        }
        return updated;
    }

    private boolean isDependencyReady(String status) {
        return STATUS_GENERATED.equals(status) || STATUS_APPROVED.equals(status);
    }

    private List<PlanStep> approveGeneratedChunks(List<PlanStep> plan) {
        List<PlanStep> updated = new ArrayList<>();
        for (PlanStep chunk : plan) {
            String status = chunk.status();
            if (STATUS_GENERATED.equals(status) || STATUS_RUNNING.equals(status) || STATUS_REPAIRING.equals(status)) {
                updated.add(chunk.withStatus(STATUS_APPROVED));
            } else {
                updated.add(chunk);
            }
        }
        return updated;
    }

    private Optional<PlanStep> findChunk(List<PlanStep> plan, String chunkId) {
        return plan.stream()
                .filter(chunk -> chunkId.equals(chunk.id()))
                .findFirst();
    }

    private PlanStep step(String id, String task, List<String> dependsOn) {
        return new PlanStep(id, task, dependsOn, STATUS_PENDING);
    }

    private Map<String, Object> routeEnd() {
        return Map.of(DdslState.KEY_ORCHESTRATOR_ROUTE, ROUTE_END);
    }

    private String inferBoundedContextName(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "GeneratedDomain";
        }

        Matcher matcher = Pattern.compile("(?i)\\bBoundedContext\\s+([A-Za-z][A-Za-z0-9_]*)").matcher(userInput);
        if (matcher.find()) {
            return matcher.group(1);
        }

        List<String> words = Pattern.compile("[A-Za-z][A-Za-z0-9]*")
                .matcher(userInput)
                .results()
                .map(MatchResult::group)
                .filter(w -> w.length() > 2)
                .filter(w -> !List.of("the", "and", "for", "with", "domain", "system", "create", "build", "thi", "mot", "cho")
                        .contains(w.toLowerCase(Locale.ROOT)))
                .limit(2)
                .toList();
        if (words.isEmpty()) {
            return "GeneratedDomain";
        }
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    private record MergeResult(String dsl, Map<String, Map<String, Integer>> lineMap) {
    }
}
