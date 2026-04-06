<#-- FreeMarker template for Domain Entity generation with business logic -->

<#-- Package declaration -->
<#if packageName?? && (packageName?length > 0)>
    package ${packageName};

</#if>

<#-- Standard imports -->
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

<#-- Custom imports -->
<#if sortedImports?has_content>
    <#list sortedImports as import>
        <#if (import?length > 0) && !import?starts_with("java.util") && !import?starts_with("java.time") && !import?starts_with("java.math.BigDecimal")>
            import ${import};
        </#if>
    </#list>

</#if>

<#-- Entity javadoc -->
/**
* Generated Domain Entity: ${className}
<#if javadoc?? && (javadoc?length > 0)>
    * ${javadoc}
</#if>
*
* This entity follows DDD tactical patterns:
* - Identity-based equality
* - Rich domain behavior
* - Business rule enforcement
* - Immutable value objects
*/

<#-- Entity annotations -->
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
<#if annotations?has_content>
    <#list annotations as annotation>
        ${annotation}
    </#list>
</#if>

<#-- Entity class declaration -->
public
<#if isAbstract?? && isAbstract>abstract </#if>
<#if isFinal?? && isFinal>final </#if>
class ${className}
<#if superClass?? && (superClass?length > 0)> extends ${superClass}</#if> {

<#-- Identity field -->
@EqualsAndHashCode.Include
@Getter
private final ${className}Id id;

<#-- Other fields -->
<#if fields?has_content>
    <#list fields as field>
        <#if field.name != "id">
        <#-- Field annotations -->
            <#if field.annotations?has_content>
                <#list field.annotations as annotation>
                    ${annotation}
                </#list>
            </#if>
            @Getter
            ${field.declaration}

        </#if>
    </#list>
</#if>

<#-- Domain events -->
<#if isAggregateRoot?? && isAggregateRoot>
@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();

    </#if>

    <#-- Constructors -->
    <#if constructors?has_content>
        <#list constructors as constructor>
            /**
            * Domain entity constructor with business validation
            */
            ${constructor.signature} {
            <#if constructor.formattedBody?? && (constructor.formattedBody?length > 0)>
                ${constructor.formattedBody?replace('\n', '\n        ')}
            <#else>
                validateConstructorArguments();
                this.id = id;

                <#if fields?has_content>
                    <#list fields as field>
                        <#if field.name != "id">
                            this.${field.name} = ${field.name};
                        </#if>
                    </#list>
                </#if>

                enforceInvariants();
            </#if>
            }

        </#list>
    </#if>

    <#-- Business methods -->
    <#if methods?has_content>
        <#list methods as method>
            /**
            * Business method: ${method.name}
            <#if method.javadoc?? && (method.javadoc?length > 0)>
                * ${method.javadoc}
            </#if>
            */
            ${method.signature}
            <#if method.hasImplementation()> {
                validateBusinessRules();

                <#if method.formattedBody?? && (method.formattedBody?length > 0)>
                    ${method.formattedBody?replace('\n', '\n        ')}
                <#else>
                    // TODO: Implement business logic
                    <#if method.returnType != "void">
                        return null;
                    </#if>
                </#if>

                enforceInvariants();

                <#if isAggregateRoot?? && isAggregateRoot>
                    raiseDomainEvent(new ${className}${method.name?cap_first}Event(this.id));
                </#if>
                }
            <#else>
                ;
            </#if>

        </#list>
    </#if>

    /**
    * Business rule validation
    */
    private void validateBusinessRules() {
    <#if fields?has_content>
        <#list fields as field>
            <#if field.type == "String">
                if ((${field.name} == null) || (${field.name}.trim().isEmpty())) {
                throw new IllegalArgumentException("${field.name} cannot be null or empty");
                }
            <#elseif field.type == "BigDecimal">
                if ((${field.name} != null) && (${field.name}.compareTo(BigDecimal.ZERO) < 0)) {
                throw new IllegalArgumentException("${field.name} cannot be negative");
                }
            </#if>
        </#list>
    </#if>
    }

    /**
    * Enforce entity invariants
    */
    private void enforceInvariants() {
    if (id == null) {
    throw new IllegalStateException("Entity must have a valid identity");
    }
    }

    private void validateConstructorArguments() {
    // Custom constructor validation
    }

    <#-- Domain events support -->
    <#if isAggregateRoot?? && isAggregateRoot>
        protected void raiseDomainEvent(DomainEvent event) {
        domainEvents.add(event);
        }

        public void clearDomainEvents() {
        domainEvents.clear();
        }

    </#if>

    @Override
    public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    ${className} that = (${className}) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "${className}{"
    + "id=" + id
    <#if fields?has_content>
        <#list fields as field>
            <#if field.name != "id">
                + ", ${field.name}=" + ${field.name}
            </#if>
        </#list>
    </#if>
    + '}';
    }
    }
