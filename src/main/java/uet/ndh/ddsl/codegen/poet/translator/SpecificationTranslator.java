package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;

/**
 * Translates SpecificationDecl AST nodes to JavaPoet TypeSpecs.
 *
 * Each specification becomes a class implementing the DDD Specification pattern:
 * <pre>
 *   public class CancellableOrders {
 *       public boolean isSatisfiedBy(Order candidate) { ... }
 *   }
 * </pre>
 */
public class SpecificationTranslator {

    private final TypeMapper typeMapper;
    private final ExpressionTranslator expressionTranslator;

    public SpecificationTranslator(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
        this.expressionTranslator = new ExpressionTranslator(typeMapper);
    }

    /**
     * Translate a specification declaration into a Java class.
     */
    public CodeArtifact translate(SpecificationDecl spec) {
                String packageName = typeMapper.packageForSpecifications();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(spec.name())
                .addModifiers(Modifier.PUBLIC);

        // Javadoc
        if (spec.documentation() != null && !spec.documentation().isBlank()) {
            classBuilder.addJavadoc("$L\n", spec.documentation());
        } else {
            classBuilder.addJavadoc("Specification: $L\n", spec.name());
            if (spec.targetType() != null) {
                classBuilder.addJavadoc("\n<p>Matches {@code $L} where the predicate is satisfied.</p>\n",
                        spec.targetType().name());
            }
        }

        // Resolve the target type name for the parameter
        TypeName targetTypeName = ClassName.get("java.lang", "Object");
        String paramName = "candidate";
        if (spec.targetType() != null) {
            targetTypeName = typeMapper.mapType(spec.targetType());
            paramName = decapitalize(spec.targetType().name());
        }

        // Build isSatisfiedBy(T candidate) method
        MethodSpec.Builder method = MethodSpec.methodBuilder("isSatisfiedBy")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(targetTypeName, paramName);

        // Add extra specification parameters (e.g. "given minAmount")
        for (ParameterDecl param : spec.parameters()) {
            TypeName paramType = typeMapper.mapType(param.type());
            method.addParameter(paramType, param.name());
        }

        // Translate the predicate expression into the method body
        if (spec.predicate() != null) {
            CodeBlock predicateCode = expressionTranslator.translateExpression(spec.predicate());
            method.addStatement("return $L", predicateCode);
        } else {
            method.addComment("No predicate defined");
            method.addStatement("return true");
        }

        classBuilder.addMethod(method.build());

        // Static factory convenience: and(), or(), not() combinators
        addAndCombinator(classBuilder, spec.name(), targetTypeName, paramName);
        addOrCombinator(classBuilder, spec.name(), targetTypeName, paramName);
        addNotFactory(classBuilder, spec.name(), targetTypeName, paramName);

        // Build the JavaFile
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();

        return new CodeArtifact(
                spec.name(),
                packageName,
                javaFile.toString(),
                CodeArtifact.ArtifactType.SPECIFICATION
        );
    }

    /**
     * Generates an {@code and()} combinator method:
     * <pre>
     *   public CancellableOrders and(CancellableOrders other) {
     *       return new CancellableOrders() {
     *           public boolean isSatisfiedBy(Order candidate) {
     *               return CancellableOrders.this.isSatisfiedBy(candidate)
     *                   && other.isSatisfiedBy(candidate);
     *           }
     *       };
     *   }
     * </pre>
     */
    private void addAndCombinator(TypeSpec.Builder classBuilder, String specName,
                                   TypeName targetType, String paramName) {
        ClassName selfType = ClassName.bestGuess(specName);

        TypeSpec anonClass = TypeSpec.anonymousClassBuilder("")
                .superclass(selfType)
                .addMethod(MethodSpec.methodBuilder("isSatisfiedBy")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(boolean.class)
                        .addParameter(targetType, paramName)
                        .addStatement("return $L.this.isSatisfiedBy($N) && other.isSatisfiedBy($N)",
                                specName, paramName, paramName)
                        .build())
                .build();

        classBuilder.addMethod(MethodSpec.methodBuilder("and")
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(selfType, "other")
                .addStatement("return $L", anonClass)
                .build());
    }

    /**
     * Generates an {@code or()} combinator method.
     */
    private void addOrCombinator(TypeSpec.Builder classBuilder, String specName,
                                  TypeName targetType, String paramName) {
        ClassName selfType = ClassName.bestGuess(specName);

        TypeSpec anonClass = TypeSpec.anonymousClassBuilder("")
                .superclass(selfType)
                .addMethod(MethodSpec.methodBuilder("isSatisfiedBy")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(boolean.class)
                        .addParameter(targetType, paramName)
                        .addStatement("return $L.this.isSatisfiedBy($N) || other.isSatisfiedBy($N)",
                                specName, paramName, paramName)
                        .build())
                .build();

        classBuilder.addMethod(MethodSpec.methodBuilder("or")
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(selfType, "other")
                .addStatement("return $L", anonClass)
                .build());
    }

    /**
     * Generates a static {@code not()} factory:
     * <pre>
     *   public static CancellableOrders not(CancellableOrders spec) { ... }
     * </pre>
     */
    private void addNotFactory(TypeSpec.Builder classBuilder, String specName,
                                TypeName targetType, String paramName) {
        ClassName selfType = ClassName.bestGuess(specName);

        TypeSpec anonClass = TypeSpec.anonymousClassBuilder("")
                .superclass(selfType)
                .addMethod(MethodSpec.methodBuilder("isSatisfiedBy")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(boolean.class)
                        .addParameter(targetType, paramName)
                        .addStatement("return !spec.isSatisfiedBy($N)", paramName)
                        .build())
                .build();

        classBuilder.addMethod(MethodSpec.methodBuilder("not")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(selfType)
                .addParameter(selfType, "spec")
                .addStatement("return $L", anonClass)
                .build());
    }

    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
