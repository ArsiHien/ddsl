package uet.ndh.ddsl.codegen;

import java.util.Objects;

/**
 * Value object representing a generated code artifact.
 * 
 * This is the output of both JavaPoet-based and FreeMarker-based code generation.
 * It contains all information needed to write the artifact to disk.
 */
public record CodeArtifact(
    String typeName,
    String packageName,
    String sourceCode,
    ArtifactType artifactType
) {
    
    public CodeArtifact {
        Objects.requireNonNull(typeName, "typeName cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(sourceCode, "sourceCode cannot be null");
        Objects.requireNonNull(artifactType, "artifactType cannot be null");
    }
    
    /**
     * Get the fully qualified name of this artifact.
     */
    public String fullyQualifiedName() {
        return packageName + "." + typeName;
    }
    
    /**
     * Get the file name for this artifact.
     */
    public String fileName() {
        return typeName + ".java";
    }
    
    /**
     * Get the relative file path (using / separator).
     */
    public String relativePath() {
        return packageName.replace('.', '/') + "/" + fileName();
    }
    
    /**
     * Check if this is a scaffold artifact (base interface/class).
     */
    public boolean isScaffold() {
        return artifactType == ArtifactType.INTERFACE 
            || artifactType == ArtifactType.ABSTRACT_CLASS
            || artifactType == ArtifactType.BASE_CLASS;
    }
    
    /**
     * Check if this is a domain model artifact.
     */
    public boolean isDomainModel() {
        return artifactType == ArtifactType.AGGREGATE_ROOT
            || artifactType == ArtifactType.ENTITY
            || artifactType == ArtifactType.VALUE_OBJECT
            || artifactType == ArtifactType.DOMAIN_EVENT;
    }
    
    /**
     * Types of code artifacts.
     */
    public enum ArtifactType {
        // Scaffold types (FreeMarker generated)
        INTERFACE,
        ABSTRACT_CLASS,
        BASE_CLASS,
        
        // Domain model types (JavaPoet generated)
        AGGREGATE_ROOT,
        ENTITY,
        VALUE_OBJECT,
        DOMAIN_EVENT,
        DOMAIN_SERVICE,
        REPOSITORY,
        FACTORY,
        SPECIFICATION,
        
        // Application layer types
        APPLICATION_SERVICE,
        USE_CASE,
        COMMAND,
        QUERY,
        
        // Infrastructure types
        REPOSITORY_IMPL,
        ADAPTER,
        
        // Utility types
        ENUM,
        CLASS,
        EXCEPTION
    }
}
