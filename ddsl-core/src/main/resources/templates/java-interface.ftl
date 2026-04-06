<#-- FreeMarker template for Java interface generation -->
<#-- Package declaration -->
<#if packageName?? && (packageName?length > 0)>
package ${packageName};

</#if>
<#-- Imports -->
<#if sortedImports?? && (sortedImports?size > 0)>
<#list sortedImports as import>
<#if (import?length > 0)>
import ${import};
</#if>
</#list>

</#if>
<#-- Interface javadoc -->
<#if javadoc?? && (javadoc?length > 0)>
/**
 * ${javadoc}
 */
<#else>
/**
 * Generated interface: ${interfaceName}
 */
</#if>
<#-- Interface annotations -->
<#if annotations?? && (annotations?size > 0)>
<#list annotations as annotation>
${annotation}
</#list>
</#if>
<#-- Interface declaration -->
public interface ${interfaceName}<#if superInterfaces?? && (superInterfaces?size > 0)> extends <#list superInterfaces as superInterface>${superInterface}<#if superInterface_has_next>, </#if></#list></#if> {

<#-- Interface methods -->
<#if methods?? && (methods?size > 0)>
<#list methods as method>
    <#-- Method annotations -->
    <#if method.annotations?? && (method.annotations?size > 0)>
    <#list method.annotations as annotation>
    ${annotation}
    </#list>
    </#if>
    <#-- Method javadoc -->
    <#if method.javadoc?? && (method.javadoc?length > 0)>
    /**
     * ${method.javadoc}
     */
    </#if>
    <#-- Method declaration (abstract by default in interfaces) -->
    ${method.signature};

</#list>
</#if>
}
