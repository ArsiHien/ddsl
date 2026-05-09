package uet.ndh.ddsl.lsp.diagram;

import lombok.extern.slf4j.Slf4j;
import uet.ndh.ddsl.ast.application.ApplicationServiceDecl;
import uet.ndh.ddsl.ast.application.UseCaseDecl;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.BoundedContextDecl;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.event.EventHandlerContainerDecl;
import uet.ndh.ddsl.ast.model.event.EventHandlerDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds diagram data models from DDSL source for extension-side Mermaid rendering.
 */
@Slf4j
public class DdslDiagramService {

    private static final String TYPE_COMPONENT = "component";
    private static final String TYPE_EVENT_FLOW = "eventFlow";

    public DiagramResponse generateComponentDiagram(String source, String sourceName) {
        try {
            log.debug("Generating component diagram for source: {}", sourceName);
            DomainModel model = parseDomainModel(source, sourceName);
            List<ComponentContextModel> contexts = model.boundedContexts().stream()
                    .map(this::toComponentContext)
                    .toList();
            log.info("Component diagram generated for source {} with {} bounded context(s)", sourceName, contexts.size());
            return DiagramResponse.success(TYPE_COMPONENT, new ComponentDiagramModel(contexts));
        } catch (ParseException e) {
            log.warn("Component diagram parse failed for source {}: {}", sourceName, e.getMessage());
            return DiagramResponse.failure(TYPE_COMPONENT, parseExceptionErrors(e));
        } catch (Exception e) {
            log.error("Component diagram generation failed for source {}", sourceName, e);
            return DiagramResponse.failure(TYPE_COMPONENT, "Failed to generate component diagram: " + exceptionSummary(e));
        }
    }

    public DiagramResponse generateEventFlowDiagram(String source, String sourceName) {
        try {
            log.debug("Generating event-flow diagram for source: {}", sourceName);
            DomainModel model = parseDomainModel(source, sourceName);
            List<EventFlowContextModel> contexts = model.boundedContexts().stream()
                    .map(this::toEventFlowContext)
                    .toList();
            log.info("Event-flow diagram generated for source {} with {} bounded context(s)", sourceName, contexts.size());
            return DiagramResponse.success(TYPE_EVENT_FLOW, new EventFlowDiagramModel(contexts));
        } catch (ParseException e) {
            log.warn("Event-flow diagram parse failed for source {}: {}", sourceName, e.getMessage());
            return DiagramResponse.failure(TYPE_EVENT_FLOW, parseExceptionErrors(e));
        } catch (Exception e) {
            log.error("Event-flow diagram generation failed for source {}", sourceName, e);
            return DiagramResponse.failure(TYPE_EVENT_FLOW, "Failed to generate event-flow diagram: " + exceptionSummary(e));
        }
    }

    private String exceptionSummary(Exception e) {
        if (e == null) {
            return "unknown error";
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + message;
    }

    private DomainModel parseDomainModel(String source, String sourceName) throws ParseException {
        if (source == null || source.isBlank()) {
            throw new ParseException("DDSL source is empty");
        }
        return new DdslParser(source, sourceName != null ? sourceName : "<input>").parse();
    }

    private List<String> parseExceptionErrors(ParseException e) {
        if (e.hasErrors()) {
            return e.getErrors().stream()
                    .map(err -> "line %d:%d - %s".formatted(
                            err.location() != null ? err.location().startLine() : 0,
                            err.location() != null ? err.location().startColumn() : 0,
                            err.message()))
                    .toList();
        }
        return List.of(e.getMessage());
    }

    private ComponentContextModel toComponentContext(BoundedContextDecl context) {
        List<ComponentNode> components = new ArrayList<>();

        components.addAll(extractAggregateComponents(context));
        components.addAll(extractDomainServiceComponents(context));
        components.addAll(extractRepositoryComponents(context));
        components.addAll(extractApplicationServiceComponents(context));

        return new ComponentContextModel(context.name(), components);
    }

    private List<ComponentNode> extractAggregateComponents(BoundedContextDecl context) {
        List<ComponentNode> components = new ArrayList<>();

        for (AggregateDecl aggregate : context.aggregates()) {
            components.add(buildAggregateNode(aggregate));
            components.addAll(buildEntityNodes(aggregate));
            components.addAll(buildValueObjectNodes(aggregate));
        }

        return components;
    }

    private ComponentNode buildAggregateNode(AggregateDecl aggregate) {
        List<BehaviorNode> behaviors = collectAggregateBehaviors(aggregate);
        List<FieldNode> fields = aggregate.root() != null
                ? toFieldNodes(aggregate.root().fields())
                : List.of();
        List<MethodNode> methods = aggregate.root() != null
                ? toMethodNodes(aggregate.root().methods())
                : List.of();

        return new ComponentNode(
                aggregate.name(),
                "aggregate",
                aggregate.name(),
                null,
                fields,
                methods,
                behaviors,
                List.of()
        );
    }

    private List<BehaviorNode> collectAggregateBehaviors(AggregateDecl aggregate) {
        List<BehaviorNode> behaviors = new ArrayList<>(toBehaviorNodes(aggregate.behaviors()));
        if (aggregate.root() != null) {
            behaviors.addAll(toBehaviorNodes(aggregate.root().behaviors()));
        }
        return behaviors;
    }

    private List<ComponentNode> buildEntityNodes(AggregateDecl aggregate) {
        List<ComponentNode> nodes = new ArrayList<>();

        for (EntityDecl entity : aggregate.entities()) {
            nodes.add(new ComponentNode(
                    aggregate.name() + "." + entity.name(),
                    "entity",
                    entity.name(),
                    aggregate.name(),
                    toFieldNodes(entity.fields()),
                    toMethodNodes(entity.methods()),
                    toBehaviorNodes(entity.behaviors()),
                    List.of()
            ));
        }

        return nodes;
    }

    private List<ComponentNode> buildValueObjectNodes(AggregateDecl aggregate) {
        List<ComponentNode> nodes = new ArrayList<>();

        for (ValueObjectDecl valueObject : aggregate.valueObjects()) {
            nodes.add(new ComponentNode(
                    aggregate.name() + "." + valueObject.name(),
                    "valueObject",
                    valueObject.name(),
                    aggregate.name(),
                    toFieldNodes(valueObject.fields()),
                    toMethodNodes(valueObject.methods()),
                    List.of(),
                    List.of()
            ));
        }

        return nodes;
    }

    private List<ComponentNode> extractDomainServiceComponents(BoundedContextDecl context) {
        List<ComponentNode> components = new ArrayList<>();

        for (DomainServiceDecl service : context.domainServices()) {
            List<String> dependencies = extractDependencies(service);
            components.add(new ComponentNode(
                    service.name(),
                    "domainService",
                    service.name(),
                    null,
                    List.of(),
                    List.of(),
                    toBehaviorNodes(service.behaviors()),
                    dependencies
            ));
        }

        return components;
    }

    private List<String> extractDependencies(DomainServiceDecl service) {
        return service.dependencies().stream()
                .map(dep -> dep.type() != null ? dep.type().name() : dep.name())
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    private List<ComponentNode> extractRepositoryComponents(BoundedContextDecl context) {
        List<ComponentNode> components = new ArrayList<>();

        for (RepositoryDecl repository : context.repositories()) {
            String owner = repository.aggregateType() != null ? repository.aggregateType().name() : null;
            components.add(new ComponentNode(
                    repository.name(),
                    "repository",
                    repository.name(),
                    owner,
                    List.of(),
                    List.of(),
                    List.of(),
                    owner != null ? List.of(owner) : List.of()
            ));
        }

        return components;
    }

    private List<ComponentNode> extractApplicationServiceComponents(BoundedContextDecl context) {
        List<ComponentNode> components = new ArrayList<>();

        for (ApplicationServiceDecl appService : context.applicationServices()) {
            components.add(buildApplicationServiceNode(appService));
            components.addAll(buildUseCaseNodes(appService));
        }

        return components;
    }

    private ComponentNode buildApplicationServiceNode(ApplicationServiceDecl appService) {
        List<String> dependencies = appService.dependencies().stream()
                .map(dep -> dep.type() != null ? dep.type().name() : dep.name())
                .filter(s -> s != null && !s.isBlank())
                .toList();

        return new ComponentNode(
                appService.name(),
                "applicationService",
                appService.name(),
                null,
                List.of(),
                List.of(),
                List.of(),
                dependencies
        );
    }

    private List<ComponentNode> buildUseCaseNodes(ApplicationServiceDecl appService) {
        List<ComponentNode> nodes = new ArrayList<>();

        for (UseCaseDecl useCase : appService.useCases()) {
            nodes.add(new ComponentNode(
                    appService.name() + "." + useCase.name(),
                    "useCase",
                    useCase.name(),
                    appService.name(),
                    List.of(),
                    List.of(),
                    toBehaviorNodes(useCase.behaviors()),
                    List.of()
            ));
        }

        return nodes;
    }

    private EventFlowContextModel toEventFlowContext(BoundedContextDecl context) {
        EventFlowCollector collector = new EventFlowCollector(context);
        return collector.collect();
    }

    private class EventFlowCollector {
        private final BoundedContextDecl context;
        private final Map<String, String> eventToAggregate = new LinkedHashMap<>();
        private final Map<String, EventHandlerInfo> eventToHandler = new LinkedHashMap<>();
        private final List<EventFlowEdge> flows = new ArrayList<>();
        private final Set<String> emittedEventNames = new LinkedHashSet<>();

        EventFlowCollector(BoundedContextDecl context) {
            this.context = context;
        }

        EventFlowContextModel collect() {
            buildEventToAggregateMap();
            buildEventHandlerIndex();
            collectAllBehaviorFlows();
            ensureEmittedEventsTracked();
            List<EventNode> events = buildEventNodes();
            return new EventFlowContextModel(context.name(), events, flows);
        }

        private void buildEventToAggregateMap() {
            for (DomainEventDecl event : context.domainEvents()) {
                String aggregateName = event.sourceAggregate() != null ? event.sourceAggregate().name() : null;
                eventToAggregate.put(event.name(), aggregateName);
            }

            for (AggregateDecl aggregate : context.aggregates()) {
                for (DomainEventDecl event : aggregate.events()) {
                    eventToAggregate.putIfAbsent(event.name(), aggregate.name());
                }
            }
        }

        private void buildEventHandlerIndex() {
            log.debug("Building event handler index for context: {}, eventHandlers count: {}",
                    context.name(), context.eventHandlers().size());
            for (EventHandlerContainerDecl container : context.eventHandlers()) {
                log.debug("Processing handler container with {} handlers", container.handlers().size());
                for (EventHandlerDecl handler : container.handlers()) {
                    String eventName = handler.targetEventName();
                    log.debug("Found handler: {} for event: {}", handler.name(), eventName);
                    if (eventName != null && !eventName.isBlank()) {
                        EventHandlerInfo info = new EventHandlerInfo(
                                handler.name(),
                                context.name(),
                                "boundedContext"
                        );
                        eventToHandler.put(eventName, info);
                        log.debug("Registered handler for event: {}", eventName);
                    }
                }
            }
            log.debug("Event handler index built with {} entries", eventToHandler.size());
        }

        private void collectAllBehaviorFlows() {
            for (AggregateDecl aggregate : context.aggregates()) {
                collectBehaviorFlows(aggregate.name(), "aggregate", aggregate.name(), aggregate.behaviors());
                if (aggregate.root() != null) {
                    collectBehaviorFlows(aggregate.name(), "aggregateRoot", aggregate.root().name(), aggregate.root().behaviors());
                }
                for (EntityDecl entity : aggregate.entities()) {
                    collectBehaviorFlows(aggregate.name(), "entity", entity.name(), entity.behaviors());
                }
            }

            for (DomainServiceDecl service : context.domainServices()) {
                collectBehaviorFlows(null, "domainService", service.name(), service.behaviors());
            }

            for (ApplicationServiceDecl appService : context.applicationServices()) {
                for (UseCaseDecl useCase : appService.useCases()) {
                    collectBehaviorFlows(null, "useCase", appService.name() + "." + useCase.name(), useCase.behaviors());
                }
            }
        }

        private void collectBehaviorFlows(
                String ownerAggregate,
                String componentType,
                String componentName,
                List<BehaviorDecl> behaviors
        ) {
            for (BehaviorDecl behavior : behaviors) {
                if (behavior.emitClause() == null || behavior.emitClause().eventName() == null || behavior.emitClause().eventName().isBlank()) {
                    continue;
                }
                String eventName = behavior.emitClause().eventName();
                String aggregateName = resolveAggregateName(eventName, ownerAggregate);

                emittedEventNames.add(eventName);
                flows.add(new EventFlowEdge(
                        componentType,
                        componentName,
                        behavior.getName(),
                        eventName,
                        aggregateName
                ));
            }
        }

        private String resolveAggregateName(String eventName, String ownerAggregate) {
            String aggregateName = eventToAggregate.get(eventName);
            if (aggregateName == null && ownerAggregate != null) {
                aggregateName = ownerAggregate;
                eventToAggregate.put(eventName, aggregateName);
            }
            return aggregateName;
        }

        private void ensureEmittedEventsTracked() {
            for (String eventName : emittedEventNames) {
                eventToAggregate.putIfAbsent(eventName, null);
            }
        }

        private List<EventNode> buildEventNodes() {
            log.debug("Building event nodes. Events in map: {}, Handlers in map: {}",
                    eventToAggregate.size(), eventToHandler.size());
            return eventToAggregate.entrySet().stream()
                    .map(entry -> {
                        String eventName = entry.getKey();
                        String aggregateName = entry.getValue();
                        EventHandlerInfo handler = eventToHandler.get(eventName);
                        boolean isHandled = handler != null;
                        log.debug("Event: {}, Handler: {}, isHandled: {}", eventName, handler, isHandled);
                        return new EventNode(eventName, aggregateName, handler, isHandled);
                    })
                    .toList();
        }
    }

    private List<BehaviorNode> toBehaviorNodes(List<BehaviorDecl> behaviors) {
        return behaviors.stream()
                .map(behavior -> new BehaviorNode(
                        behavior.getName(),
                        behavior.phrase() != null ? behavior.phrase().rawText() : null,
                        behavior.parameters().stream().map(this::toParameterNode).toList(),
                        behavior.emitClause() != null ? behavior.emitClause().eventName() : null
                ))
                .toList();
    }

    private ParameterNode toParameterNode(ParameterDecl parameter) {
        String type = parameter.type() != null ? parameter.type().name() : null;
        return new ParameterNode(parameter.name(), type);
    }

    private List<FieldNode> toFieldNodes(List<uet.ndh.ddsl.ast.member.FieldDecl> fields) {
        return fields.stream()
                .map(this::toFieldNode)
                .toList();
    }

    private FieldNode toFieldNode(uet.ndh.ddsl.ast.member.FieldDecl field) {
        String visibility = formatVisibility(field.visibility());
        String typeName = formatTypeRef(field.type());
        List<String> annotations = extractFieldAnnotations(field);
        return new FieldNode(field.name(), typeName, visibility, annotations, field.isFinal());
    }

    private String formatVisibility(uet.ndh.ddsl.ast.common.Visibility visibility) {
        return switch (visibility) {
            case PUBLIC -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
            default -> "package";
        };
    }

    private String formatTypeRef(uet.ndh.ddsl.ast.common.TypeRef typeRef) {
        if (typeRef == null) {
            return "void";
        }
        String baseType = typeRef.name();
        if (typeRef.typeArguments().isEmpty()) {
            return baseType;
        }
        String typeArgs = typeRef.typeArguments().stream()
                .map(this::formatTypeRef)
                .collect(java.util.stream.Collectors.joining(", "));
        return baseType + "<" + typeArgs + ">";
    }

    private List<String> extractFieldAnnotations(uet.ndh.ddsl.ast.member.FieldDecl field) {
        List<String> annotations = new ArrayList<>();
        if (field.constraints() != null) {
            for (var constraint : field.constraints()) {
                annotations.add(formatConstraint(constraint));
            }
        }
        return annotations;
    }

    private String formatConstraint(uet.ndh.ddsl.ast.common.Constraint constraint) {
        StringBuilder sb = new StringBuilder("@");
        sb.append(constraint.type().name().toLowerCase());
        if (constraint.value() != null) {
            sb.append("(").append(constraint.value()).append(")");
        }
        return sb.toString();
    }

    private List<MethodNode> toMethodNodes(List<MethodDecl> methods) {
        return methods.stream()
                .map(this::toMethodNode)
                .toList();
    }

    private MethodNode toMethodNode(MethodDecl method) {
        String visibility = formatVisibility(method.visibility());
        String returnType = formatTypeRef(method.returnType());
        List<ParameterNode> params = method.parameters().stream()
                .map(this::toParameterNode)
                .toList();
        String kind = method.kind() != null ? method.kind().name().toLowerCase() : "regular";
        return new MethodNode(method.name(), returnType, params, visibility, kind);
    }

    public record ComponentDiagramModel(List<ComponentContextModel> contexts) {
        public ComponentDiagramModel {
            contexts = contexts != null ? List.copyOf(contexts) : List.of();
        }
    }

    public record ComponentContextModel(String boundedContext, List<ComponentNode> components) {
        public ComponentContextModel {
            components = components != null ? List.copyOf(components) : List.of();
        }
    }

    public record ComponentNode(
            String id,
            String type,
            String name,
            String parent,
            List<FieldNode> fields,
            List<MethodNode> methods,
            List<BehaviorNode> behaviors,
            List<String> dependencies
    ) {
        public ComponentNode {
            fields = fields != null ? List.copyOf(fields) : List.of();
            methods = methods != null ? List.copyOf(methods) : List.of();
            behaviors = behaviors != null ? List.copyOf(behaviors) : List.of();
            dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
        }
    }

    public record BehaviorNode(String name, String phrase, List<ParameterNode> parameters, String emitsEvent) {
        public BehaviorNode {
            parameters = parameters != null ? List.copyOf(parameters) : List.of();
        }
    }

    public record ParameterNode(String name, String type) {
    }

    /**
     * Represents a field in a class-like component (aggregate, entity, value object).
     */
    public record FieldNode(
            String name,
            String type,
            String visibility,
            List<String> annotations,
            boolean isFinal
    ) {
        public FieldNode {
            annotations = annotations != null ? List.copyOf(annotations) : List.of();
        }
    }

    /**
     * Represents a method in a class-like component.
     */
    public record MethodNode(
            String name,
            String returnType,
            List<ParameterNode> parameters,
            String visibility,
            String kind
    ) {
        public MethodNode {
            parameters = parameters != null ? List.copyOf(parameters) : List.of();
        }
    }

    /**
     * Represents information about an event handler.
     */
    public record EventHandlerInfo(
            String handlerName,
            String handledByComponent,
            String handledByComponentType
    ) {
    }

    public record EventFlowDiagramModel(List<EventFlowContextModel> contexts) {
        public EventFlowDiagramModel {
            contexts = contexts != null ? List.copyOf(contexts) : List.of();
        }
    }

    public record EventFlowContextModel(String boundedContext, List<EventNode> events, List<EventFlowEdge> flows) {
        public EventFlowContextModel {
            events = events != null ? List.copyOf(events) : List.of();
            flows = flows != null ? List.copyOf(flows) : List.of();
        }
    }

    public record EventNode(
            String eventName,
            String aggregateName,
            EventHandlerInfo handler,
            boolean isHandled
    ) {
    }

    public record EventFlowEdge(
            String componentType,
            String componentName,
            String behaviorName,
            String eventName,
            String aggregateName
    ) {
    }
}
