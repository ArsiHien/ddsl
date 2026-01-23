<#-- FreeMarker template for Java class generation -->
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
<#-- Class javadoc -->
<#if javadoc?? && (javadoc?length > 0)>
/**
 * ${javadoc}
 */
<#else>
/**
 * Generated class: ${className}
 */
</#if>
<#-- Class annotations -->
<#if annotations?? && (annotations?size > 0)>
<#list annotations as annotation>
${annotation}
</#list>
</#if>
<#-- Class declaration -->
public <#if isAbstract>abstract </#if><#if isFinal>final </#if>class ${className}<#if superClass??> extends ${superClass}</#if><#if implementedInterfaces?? && (implementedInterfaces?size > 0)> implements <#list implementedInterfaces as interface>${interface}<#if interface_has_next>, </#if></#list></#if> {

<#-- Fields -->
<#if fields?? && (fields?size > 0)>
<#list fields as field>
    <#-- Field annotations -->
    <#if field.annotations?? && (field.annotations?size > 0)>
    <#list field.annotations as annotation>
    ${annotation}
    </#list>
    </#if>
    <#-- Field javadoc -->
    <#if field.javadoc?? && (field.javadoc?length > 0)>
    /**
     * ${field.javadoc}
     */
    </#if>
    <#-- Field declaration -->
    ${field.declaration}

</#list>
</#if>
<#-- Constructors -->
<#if constructors?? && (constructors?size > 0)>
<#list constructors as constructor>
    <#-- Constructor annotations -->
    <#if constructor.annotations?? && (constructor.annotations?size > 0)>
    <#list constructor.annotations as annotation>
    ${annotation}
    </#list>
    </#if>
    <#-- Constructor javadoc -->
    <#if constructor.javadoc?? && (constructor.javadoc?length > 0)>
    /**
     * ${constructor.javadoc}
     */
    </#if>
    <#-- Constructor declaration and body -->
    ${constructor.signature} {
<#if constructor.formattedBody?? && (constructor.formattedBody?length > 0)>
        ${constructor.formattedBody?replace('\n', '\n        ')}
</#if>
    }

</#list>
</#if>
<#-- Methods -->
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
    <#-- Method declaration and body -->
    ${method.signature}<#if method.hasImplementation()> {
<#if method.formattedBody?? && (method.formattedBody?length > 0)>
        ${method.formattedBody?replace('\n', '\n        ')}
</#if>
    }<#else>;</#if>

</#list>
</#if>
<#-- Inner classes -->
<#if innerClasses?? && (innerClasses?size > 0)>
<#list innerClasses as innerClass>
    <#-- Recursively include inner class template with proper indentation -->
    <#include "java-class.ftl">

</#list>
</#if>
}
