<#-- FreeMarker template for Domain Value Object generation -->
<#-- Package declaration -->
<#if packageName?? && (packageName?length > 0)>
package ${packageName};

</#if>
<#-- Standard imports for value objects -->
import lombok.Value;
import lombok.Builder;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

<#-- Custom imports -->
<#if sortedImports?? && (sortedImports?size > 0)>
<#list sortedImports as import>
<#if (import?length > 0) && !import?starts_with("java.util") && !import?starts_with("java.time") && !import?starts_with("java.math.BigDecimal")>
import ${import};
</#if>
</#list>

</#if>
<#-- Value Object javadoc -->
/**
 * Generated Domain Value Object: ${className}
 * <#if javadoc?? && (javadoc?length > 0)>
 * ${javadoc}
 * </#if>
 *
 * This value object follows DDD tactical patterns:
 * - Immutability (no setters)
 * - Value-based equality
 * - Self-validation
 * - Rich behavior methods
 * - No identity
 */
<#-- Value Object annotations -->
@Value
@Builder
<#if annotations?? && (annotations?size > 0)>
<#list annotations as annotation>
${annotation}
</#list>
</#if>
<#-- Value Object class declaration -->
public <#if isFinal>final </#if>class ${className} {

<#-- Fields (all final by @Value annotation) -->
<#if fields?? && (fields?size > 0)>
<#list fields as field>
    <#-- Field javadoc -->
    <#if field.javadoc?? && (field.javadoc?length > 0)>
    /**
     * ${field.javadoc}
     */
    </#if>
    <#-- Field annotations -->
    <#if field.annotations?? && (field.annotations?size > 0)>
    <#list field.annotations as annotation>
    ${annotation}
    </#list>
    </#if>
    ${field.type} ${field.name};

</#list>
</#if>
<#-- Value Object validation constructor -->
    /**
     * Creates a new ${className} with validation.
     * All value objects must be created in a valid state.
     */
    private ${className}(<#if fields?? && (fields?size > 0)><#list fields as field>${field.type} ${field.name}<#if field_has_next>, </#if></#list></#if>) {
<#if fields?? && (fields?size > 0)>
<#list fields as field>
        this.${field.name} = ${field.name};
</#list>
</#if>

        // Self-validation - Value objects must always be in valid state
        validate();
    }

    /**
     * Factory method for creating validated ${className} instances.
     */
    public static ${className} of(<#if fields?? && (fields?size > 0)><#list fields as field>${field.type} ${field.name}<#if field_has_next>, </#if></#list></#if>) {
        return new ${className}(<#if fields?? && (fields?size > 0)><#list fields as field>${field.name}<#if field_has_next>, </#if></#list></#if>);
    }

<#-- Business methods with rich domain logic -->
<#if methods?? && (methods?size > 0)>
<#list methods as method>
    /**
     * Business behavior: ${method.name}
     * <#if method.javadoc?? && (method.javadoc?length > 0)>
     * ${method.javadoc}
     * </#if>
     *
     * Value objects should contain rich business behavior related to their data.
     */
    ${method.signature}<#if method.hasImplementation()> {
<#if method.formattedBody?? && (method.formattedBody?length > 0)>
        ${method.formattedBody?replace('\n', '\n        ')}
<#else>
        // TODO: Implement business behavior for ${method.name}
        <#if method.returnType != "void">
        return null; // Replace with actual business logic
        </#if>
</#if>
    }<#else>;</#if>

</#list>
</#if>
    /**
     * Validation method - ensures value object is always in valid state.
     * Throws IllegalArgumentException if invalid.
     */
    private void validate() {
<#if fields?? && (fields?size > 0)>
<#list fields as field>
    <#if field.type == "String">
        if ((${field.name} == null) || (${field.name}.trim().isEmpty())) {
            throw new IllegalArgumentException("${className}.${field.name} cannot be null or empty");
        }
    <#elseif field.type == "BigDecimal">
        if ((${field.name} == null)) {
            throw new IllegalArgumentException("${className}.${field.name} cannot be null");
        }
        if ((${field.name}.compareTo(BigDecimal.ZERO)) < 0) {
            throw new IllegalArgumentException("${className}.${field.name} cannot be negative");
        }
    <#elseif field.type?ends_with("Collection") || field.type?starts_with("List") || field.type?starts_with("Set")>
        if ((${field.name} == null) || (${field.name}.isEmpty())) {
            throw new IllegalArgumentException("${className}.${field.name} cannot be null or empty");
        }
    <#else>
        if ((${field.name} == null)) {
            throw new IllegalArgumentException("${className}.${field.name} cannot be null");
        }
    </#if>
</#list>

        // Custom business validation rules
        validateBusinessRules();
<#else>
        // No fields to validate
</#if>
    }

    /**
     * Custom business validation rules specific to ${className}.
     * Override this method to add domain-specific validation.
     */
    private void validateBusinessRules() {
        // Add custom validation logic here
        // Example: email format validation, phone number format, etc.
    }

<#-- Common value object utility methods -->
    /**
     * Check if this value object is empty/has default values.
     */
    public boolean isEmpty() {
<#if fields?? && (fields?size > 0)>
        return <#list fields as field><#if field.type == "String">(${field.name} == null) || (${field.name}.trim().isEmpty())<#elseif field.type?ends_with("Collection") || field.type?starts_with("List") || field.type?starts_with("Set")>(${field.name} == null) || (${field.name}.isEmpty())<#else>(${field.name} == null)</#if><#if field_has_next> && </#if></#list>;
<#else>
        return true; // No fields means empty
</#if>
    }

    /**
     * Rich toString with all value object data.
     */
    @Override
    public String toString() {
        return "${className}{" +
<#if fields?? && (fields?size > 0)>
<#list fields as field>
            "${field.name}=" + ${field.name}<#if field_has_next> + ", " +</#if>
</#list> +
</#if>
            '}';
    }
}
