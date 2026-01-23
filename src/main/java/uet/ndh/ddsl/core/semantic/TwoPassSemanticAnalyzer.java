package uet.ndh.ddsl.core.semantic;

import lombok.Data;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.BoundedContext;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.core.ValidationError;

import java.util.*;

/**
 * Two-pass semantic analyzer implementing classical compiler theory techniques.
 *
 * Pass 1: Declaration Collection (Symbol Table Construction)
 * - Build symbol table with all declarations
 * - Establish scope hierarchy
 * - Detect naming conflicts
 *
 * Pass 2: Reference Resolution & Semantic Validation
 * - Resolve all symbol references
 * - Type checking and constraint validation
 * - Graph-based dependency analysis
 *
 * Academic references:
 * - Aho, Sethi, Ullman: Compilers - Principles, Techniques, and Tools
 * - Cooper, Torczon: Engineering a Compiler (Symbol tables and semantic analysis)
 */
@Component
@Data
public class TwoPassSemanticAnalyzer {

    private final SymbolTable symbolTable;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final SemanticNormalizer normalizer;
    private final List<ValidationError> semanticErrors;

    public TwoPassSemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.dependencyAnalyzer = new DependencyAnalyzer();
        this.normalizer = new SemanticNormalizer();
        this.semanticErrors = new ArrayList<>();
    }

    /**
     * Main semantic analysis entry point
     * Implements classical two-pass algorithm with normalization
     */
    public SemanticAnalysisResults analyze(DomainModel model) {
        System.out.println("🔍 Starting Two-Pass Semantic Analysis...");
        long startTime = System.currentTimeMillis();

        try {
            // Pre-pass: Normalization/Desugaring
            System.out.println("\n📋 Pre-pass: Semantic Normalization");
            SemanticNormalizer.NormalizedDomainModel normalizedModel = normalizer.normalize(model);

            // Pass 1: Declaration Collection
            System.out.println("\n📋 Pass 1: Declaration Collection (Symbol Table Construction)");
            collectDeclarations(normalizedModel.getModel());

            // Pass 2: Reference Resolution and Validation
            System.out.println("\n📋 Pass 2: Reference Resolution & Semantic Validation");
            resolveReferencesAndValidate(normalizedModel.getModel());

            // Post-pass: Graph Analysis
            System.out.println("\n📋 Post-pass: Graph-based Dependency Analysis");
            DependencyAnalyzer.SemanticAnalysisResult graphAnalysis =
                    dependencyAnalyzer.analyze(normalizedModel.getModel(), symbolTable);

            long endTime = System.currentTimeMillis();
            System.out.printf("✅ Semantic analysis completed in %d ms\n", endTime - startTime);

            return new SemanticAnalysisResults(
                    symbolTable,
                    normalizedModel,
                    semanticErrors,
                    graphAnalysis,
                    endTime - startTime
            );

        } catch (SemanticException e) {
            semanticErrors.add(new ValidationError(
                    "Semantic analysis failed: " + e.getMessage(),
                    e.getLocation(),
                    ValidationError.Severity.ERROR
            ));

            long endTime = System.currentTimeMillis();
            return new SemanticAnalysisResults(
                    symbolTable,
                    null,
                    semanticErrors,
                    null,
                    endTime - startTime
            );
        }
    }

    /**
     * Pass 1: Declaration Collection
     * Algorithm: Depth-first traversal with scope management
     * Builds symbol table for name resolution
     */
    private void collectDeclarations(DomainModel model) {
        System.out.println("   🏗️  Building symbol table...");

        // Process each bounded context
        for (BoundedContext context : model.getBoundedContexts()) {
            processContextDeclarations(context);
        }

        System.out.printf("   ✅ Collected %d symbols across %d scopes\n",
                         symbolTable.getGlobalSymbols().size(),
                         getCurrentScopeDepth());
    }

    /**
     * Process declarations within a bounded context
     */
    private void processContextDeclarations(BoundedContext context) {
        // Enter context scope
        symbolTable.pushScope(context.getName());

        try {
            // Declare context itself
            symbolTable.declare(context.getName(),
                               SymbolTable.SymbolType.BOUNDED_CONTEXT,
                               context.getLocation(),
                               context);

            // Process aggregates
            for (Aggregate aggregate : context.getAggregates()) {
                processAggregateDeclarations(aggregate, context);
            }

            // Process value objects
            for (ValueObject valueObject : context.getValueObjects()) {
                symbolTable.declare(valueObject.getName(),
                                  SymbolTable.SymbolType.VALUE_OBJECT,
                                  valueObject.getLocation(),
                                  valueObject);
            }

            // Process domain services
            context.getDomainServices().forEach(service -> {
                symbolTable.declare(service.getName(),
                                  SymbolTable.SymbolType.DOMAIN_SERVICE,
                                  service.getLocation(),
                                  service);
            });

            // Process domain events
            context.getDomainEvents().forEach(event -> {
                symbolTable.declare(event.getName(),
                                  SymbolTable.SymbolType.DOMAIN_EVENT,
                                  event.getLocation(),
                                  event);
            });

        } catch (SemanticException e) {
            semanticErrors.add(new ValidationError(
                    e.getMessage(),
                    e.getLocation(),
                    ValidationError.Severity.ERROR
            ));
        } finally {
            // Exit context scope
            symbolTable.popScope();
        }
    }

    /**
     * Process aggregate declarations
     */
    private void processAggregateDeclarations(Aggregate aggregate, BoundedContext context) {
        // Declare aggregate
        symbolTable.declare(aggregate.getName(),
                          SymbolTable.SymbolType.AGGREGATE,
                          aggregate.getLocation(),
                          aggregate);

        // Enter aggregate scope
        symbolTable.pushScope(aggregate.getName());

        try {
            // Declare aggregate root
            Entity root = aggregate.getRoot();
            symbolTable.declare(root.getName(),
                              SymbolTable.SymbolType.ENTITY,
                              root.getLocation(),
                              root);

            // Declare internal entities
            for (Entity entity : aggregate.getEntities()) {
                symbolTable.declare(entity.getName(),
                                  SymbolTable.SymbolType.ENTITY,
                                  entity.getLocation(),
                                  entity);
            }

        } finally {
            symbolTable.popScope();
        }
    }

    /**
     * Pass 2: Reference Resolution and Validation
     * Algorithm: Symbol resolution with scope chain lookup
     */
    private void resolveReferencesAndValidate(DomainModel model) {
        System.out.println("   🔍 Resolving symbol references...");

        int resolvedReferences = 0;
        int unresolvedReferences = 0;

        // Process each bounded context
        for (BoundedContext context : model.getBoundedContexts()) {
            symbolTable.pushScope(context.getName());

            try {
                // Resolve references in aggregates
                for (Aggregate aggregate : context.getAggregates()) {
                    ReferenceResolutionResult result = resolveAggregateReferences(aggregate);
                    resolvedReferences += result.getResolvedCount();
                    unresolvedReferences += result.getUnresolvedCount();
                }

            } finally {
                symbolTable.popScope();
            }
        }

        System.out.printf("   ✅ Resolved %d references, %d unresolved\n",
                         resolvedReferences, unresolvedReferences);

        if (unresolvedReferences > 0) {
            System.out.printf("   ⚠️  %d unresolved references may indicate semantic errors\n",
                             unresolvedReferences);
        }
    }

    /**
     * Resolve references within an aggregate
     */
    private ReferenceResolutionResult resolveAggregateReferences(Aggregate aggregate) {
        int resolved = 0;
        int unresolved = 0;

        symbolTable.pushScope(aggregate.getName());

        try {
            // Resolve references in aggregate root
            ReferenceResolutionResult rootResult = resolveEntityReferences(aggregate.getRoot());
            resolved += rootResult.getResolvedCount();
            unresolved += rootResult.getUnresolvedCount();

            // Resolve references in internal entities
            for (Entity entity : aggregate.getEntities()) {
                ReferenceResolutionResult entityResult = resolveEntityReferences(entity);
                resolved += entityResult.getResolvedCount();
                unresolved += entityResult.getUnresolvedCount();
            }

        } finally {
            symbolTable.popScope();
        }

        return new ReferenceResolutionResult(resolved, unresolved);
    }

    /**
     * Resolve references within an entity
     */
    private ReferenceResolutionResult resolveEntityReferences(Entity entity) {
        int resolved = 0;
        int unresolved = 0;

        // Check field type references
        entity.getFields().forEach(field -> {
            String typeName = field.getType().getSimpleName();
            Optional<SymbolTable.SymbolEntry> symbol = symbolTable.resolve(typeName);

            if (symbol.isPresent()) {
                // Add dependency for graph analysis
                symbolTable.addDependency(entity.getName(), symbol.get().qualifiedName());
            } else if (!isPrimitiveType(typeName)) {
                // Only report unresolved if not a primitive type
                semanticErrors.add(new ValidationError(
                        String.format("Unresolved type reference: '%s' in entity '%s'",
                                     typeName, entity.getName()),
                        field.getLocation(),
                        ValidationError.Severity.WARNING
                ));
            }
        });

        return new ReferenceResolutionResult(resolved, unresolved);
    }

    /**
     * Check if a type name represents a primitive type
     */
    private boolean isPrimitiveType(String typeName) {
        return Arrays.asList("String", "int", "Integer", "long", "Long",
                           "boolean", "Boolean", "double", "Double",
                           "BigDecimal", "UUID", "Instant", "LocalDate",
                           "LocalDateTime").contains(typeName);
    }

    private int getCurrentScopeDepth() {
        return symbolTable.getScopeStack().size();
    }

    /**
     * Result of reference resolution for an entity/aggregate
     */
    @Data
    private static class ReferenceResolutionResult {
        private final int resolvedCount;
        private final int unresolvedCount;
    }

    /**
     * Complete semantic analysis results
     */
    @Data
    public static class SemanticAnalysisResults {
        private final SymbolTable symbolTable;
        private final SemanticNormalizer.NormalizedDomainModel normalizedModel;
        private final List<ValidationError> semanticErrors;
        private final DependencyAnalyzer.SemanticAnalysisResult graphAnalysis;
        private final long analysisTimeMs;

        public boolean hasErrors() {
            boolean hasSemanticErrors = semanticErrors.stream()
                    .anyMatch(error -> error.getSeverity() == ValidationError.Severity.ERROR);
            boolean hasGraphErrors = graphAnalysis != null && graphAnalysis.hasErrors();
            return hasSemanticErrors || hasGraphErrors;
        }

        public boolean hasWarnings() {
            return semanticErrors.stream()
                    .anyMatch(error -> error.getSeverity() == ValidationError.Severity.WARNING);
        }

        public int getSymbolCount() {
            return symbolTable.getGlobalSymbols().size();
        }

        public int getDependencyCount() {
            return graphAnalysis != null ?
                   graphAnalysis.getDependencyGraph().values().stream()
                           .mapToInt(Set::size).sum() : 0;
        }

        public String getSummary() {
            return String.format(
                "Semantic Analysis Summary:\n" +
                "- Symbols: %d\n" +
                "- Dependencies: %d\n" +
                "- Errors: %d\n" +
                "- Warnings: %d\n" +
                "- Analysis time: %d ms\n" +
                "- Normalizations: %s",
                getSymbolCount(),
                getDependencyCount(),
                (int) semanticErrors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(),
                (int) semanticErrors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.WARNING).count(),
                analysisTimeMs,
                normalizedModel != null ? normalizedModel.getTransformationSummary() : "None"
            );
        }
    }
}
