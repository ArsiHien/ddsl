package uet.ndh.ddsl.codegen.scaffold;

/**
 * Context for scaffold template generation.
 * 
 * Contains package names and configuration for generating base DDD building blocks.
 */
public record TemplateContext(
    String basePackage,
    String sharedPackage
) {
    
    /**
     * Create context with default shared package.
     */
    public static TemplateContext of(String basePackage) {
        return new TemplateContext(basePackage, basePackage + ".shared");
    }
    
    /**
     * Get the package for aggregate-specific code.
     */
    public String aggregatePackage(String aggregateName) {
        return basePackage + "." + aggregateName.toLowerCase();
    }
    
    /**
     * Get the package for infrastructure code.
     */
    public String infrastructurePackage() {
        return basePackage.replace(".domain", ".infrastructure");
    }
    
    /**
     * Get the package for application services.
     */
    public String applicationPackage() {
        return basePackage.replace(".domain", ".application");
    }
}
