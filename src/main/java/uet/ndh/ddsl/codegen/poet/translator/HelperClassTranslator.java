package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates reusable helper classes described in the language/codegen spec.
 */
public class HelperClassTranslator {

    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName MAP = ClassName.get(java.util.Map.class);

    private final String basePackage;

    public HelperClassTranslator(TypeMapper typeMapper) {
        this.basePackage = typeMapper.getBasePackage();
    }

    public List<CodeArtifact> generateTemporalHelpers() {
        List<CodeArtifact> artifacts = new ArrayList<>();
        String pkg = basePackage + ".temporal";

        ClassName instant = ClassName.get("java.time", "Instant");
        ClassName chronoUnit = ClassName.get("java.time.temporal", "ChronoUnit");

        TypeSpec type = TypeSpec.classBuilder("TemporalPredicates")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build())
            .addMethod(MethodSpec.methodBuilder("isBefore")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(instant, "y")
                .addStatement("return x.isBefore(y)")
                .build())
            .addMethod(MethodSpec.methodBuilder("isAfter")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(instant, "y")
                .addStatement("return x.isAfter(y)")
                .build())
            .addMethod(MethodSpec.methodBuilder("isMoreThanAgo")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(TypeName.LONG, "amount")
                .addParameter(chronoUnit, "unit")
                .addStatement("return x.isBefore($T.now().minus(amount, unit))", instant)
                .build())
            .addMethod(MethodSpec.methodBuilder("isLessThanAgo")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(TypeName.LONG, "amount")
                .addParameter(chronoUnit, "unit")
                .addStatement("return x.isAfter($T.now().minus(amount, unit))", instant)
                .build())
            .addMethod(MethodSpec.methodBuilder("isWithinNext")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(TypeName.LONG, "amount")
                .addParameter(chronoUnit, "unit")
                .addStatement("$T now = $T.now()", instant, instant)
                .addStatement("$T future = now.plus(amount, unit)", instant)
                .addStatement("return x.isAfter(now) && x.isBefore(future)")
                .build())
            .addMethod(MethodSpec.methodBuilder("isWithinLast")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(TypeName.LONG, "amount")
                .addParameter(chronoUnit, "unit")
                .addStatement("$T now = $T.now()", instant, instant)
                .addStatement("$T past = now.minus(amount, unit)", instant)
                .addStatement("return x.isAfter(past) && x.isBefore(now)")
                .build())
            .addMethod(MethodSpec.methodBuilder("isBetween")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(instant, "x")
                .addParameter(instant, "start")
                .addParameter(instant, "end")
                .addStatement("return x.isAfter(start) && x.isBefore(end)")
                .build())
            .build();

        JavaFile file = JavaFile.builder(pkg, type).skipJavaLangImports(true).indent("    ").build();
        artifacts.add(new CodeArtifact("TemporalPredicates", pkg, file.toString(), CodeArtifact.ArtifactType.CLASS));
        return artifacts;
    }

    public List<CodeArtifact> generateValidationHelpers() {
        List<CodeArtifact> artifacts = new ArrayList<>();
        String pkg = basePackage + ".validation";

        artifacts.add(generateValidationError(pkg));
        artifacts.add(generateValidationWarning(pkg));
        artifacts.add(generateValidationException(pkg));
        artifacts.add(generateValidationResult(pkg));
        artifacts.add(generateGroupedValidationResult(pkg));

        return artifacts;
    }

    public List<CodeArtifact> generateStateMachineExceptions() {
        List<CodeArtifact> artifacts = new ArrayList<>();
        String pkg = basePackage + ".statemachine";

        TypeSpec illegalTransition = TypeSpec.classBuilder("IllegalStateTransitionException")
            .addModifiers(Modifier.PUBLIC)
            .superclass(RuntimeException.class)
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "message")
                .addStatement("super(message)")
                .build())
            .build();

        TypeSpec guardException = TypeSpec.classBuilder("StateTransitionGuardException")
            .addModifiers(Modifier.PUBLIC)
            .superclass(RuntimeException.class)
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "message")
                .addStatement("super(message)")
                .build())
            .build();

        JavaFile illegalFile = JavaFile.builder(pkg, illegalTransition).skipJavaLangImports(true).indent("    ").build();
        JavaFile guardFile = JavaFile.builder(pkg, guardException).skipJavaLangImports(true).indent("    ").build();

        artifacts.add(new CodeArtifact("IllegalStateTransitionException", pkg, illegalFile.toString(), CodeArtifact.ArtifactType.EXCEPTION));
        artifacts.add(new CodeArtifact("StateTransitionGuardException", pkg, guardFile.toString(), CodeArtifact.ArtifactType.EXCEPTION));
        return artifacts;
    }

    private CodeArtifact generateValidationError(String pkg) {
        TypeSpec type = TypeSpec.classBuilder("ValidationError")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(FieldSpec.builder(String.class, "field", Modifier.PRIVATE, Modifier.FINAL).build())
            .addField(FieldSpec.builder(String.class, "message", Modifier.PRIVATE, Modifier.FINAL).build())
            .addField(FieldSpec.builder(String.class, "code", Modifier.PRIVATE, Modifier.FINAL).build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "message")
                .addStatement("this(null, message, null)")
                .build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "field")
                .addParameter(String.class, "message")
                .addStatement("this(field, message, null)")
                .build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "field")
                .addParameter(String.class, "message")
                .addParameter(String.class, "code")
                .addStatement("this.field = field")
                .addStatement("this.message = message")
                .addStatement("this.code = code")
                .build())
            .addMethod(MethodSpec.methodBuilder("getField").addModifiers(Modifier.PUBLIC).returns(String.class).addStatement("return field").build())
            .addMethod(MethodSpec.methodBuilder("getMessage").addModifiers(Modifier.PUBLIC).returns(String.class).addStatement("return message").build())
            .addMethod(MethodSpec.methodBuilder("getCode").addModifiers(Modifier.PUBLIC).returns(String.class).addStatement("return code").build())
            .build();

        JavaFile file = JavaFile.builder(pkg, type).skipJavaLangImports(true).indent("    ").build();
        return new CodeArtifact("ValidationError", pkg, file.toString(), CodeArtifact.ArtifactType.CLASS);
    }

    private CodeArtifact generateValidationWarning(String pkg) {
        TypeSpec type = TypeSpec.classBuilder("ValidationWarning")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(FieldSpec.builder(String.class, "message", Modifier.PRIVATE, Modifier.FINAL).build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "message")
                .addStatement("this.message = message")
                .build())
            .addMethod(MethodSpec.methodBuilder("getMessage")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return message")
                .build())
            .build();

        JavaFile file = JavaFile.builder(pkg, type).skipJavaLangImports(true).indent("    ").build();
        return new CodeArtifact("ValidationWarning", pkg, file.toString(), CodeArtifact.ArtifactType.CLASS);
    }

    private CodeArtifact generateValidationException(String pkg) {
        ClassName validationError = ClassName.get(pkg, "ValidationError");
        TypeName errorList = ParameterizedTypeName.get(LIST, validationError);

        TypeSpec type = TypeSpec.classBuilder("ValidationException")
            .addModifiers(Modifier.PUBLIC)
            .superclass(RuntimeException.class)
            .addField(FieldSpec.builder(errorList, "errors", Modifier.PRIVATE, Modifier.FINAL).build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(errorList, "errors")
                .addStatement("super(formatErrorMessage(errors))")
                .addStatement("this.errors = $T.copyOf(errors)", List.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("getErrors")
                .addModifiers(Modifier.PUBLIC)
                .returns(errorList)
                .addStatement("return errors")
                .build())
            .addMethod(MethodSpec.methodBuilder("formatErrorMessage")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(errorList, "errors")
                .beginControlFlow("if (errors == null || errors.isEmpty())")
                .addStatement("return $S", "Validation failed")
                .endControlFlow()
                .beginControlFlow("if (errors.size() == 1)")
                .addStatement("return errors.get(0).getMessage()")
                .endControlFlow()
                .addStatement("return $S + errors.size()", "Validation failed with errors: ")
                .build())
            .build();

        JavaFile file = JavaFile.builder(pkg, type).skipJavaLangImports(true).indent("    ").build();
        return new CodeArtifact("ValidationException", pkg, file.toString(), CodeArtifact.ArtifactType.EXCEPTION);
    }

    private CodeArtifact generateValidationResult(String pkg) {
        ClassName validationError = ClassName.get(pkg, "ValidationError");
        ClassName validationWarning = ClassName.get(pkg, "ValidationWarning");
        ClassName validationException = ClassName.get(pkg, "ValidationException");
        TypeName errorList = ParameterizedTypeName.get(LIST, validationError);
        TypeName warningList = ParameterizedTypeName.get(LIST, validationWarning);

        TypeSpec type = TypeSpec.classBuilder("ValidationResult")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(FieldSpec.builder(errorList, "errors", Modifier.PRIVATE, Modifier.FINAL).build())
            .addField(FieldSpec.builder(warningList, "warnings", Modifier.PRIVATE, Modifier.FINAL).build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(errorList, "errors")
                .addParameter(warningList, "warnings")
                .addStatement("this.errors = $T.copyOf(errors)", List.class)
                .addStatement("this.warnings = $T.copyOf(warnings)", List.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("success")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(pkg, "ValidationResult"))
                .addStatement("return new ValidationResult($T.of(), $T.of())", List.class, List.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("ofErrors")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(pkg, "ValidationResult"))
                .addParameter(errorList, "errors")
                .addStatement("return new ValidationResult(errors, $T.of())", List.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("of")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(pkg, "ValidationResult"))
                .addParameter(errorList, "errors")
                .addParameter(warningList, "warnings")
                .addStatement("return new ValidationResult(errors, warnings)")
                .build())
            .addMethod(MethodSpec.methodBuilder("isValid").addModifiers(Modifier.PUBLIC).returns(TypeName.BOOLEAN).addStatement("return errors.isEmpty()").build())
            .addMethod(MethodSpec.methodBuilder("hasErrors").addModifiers(Modifier.PUBLIC).returns(TypeName.BOOLEAN).addStatement("return !errors.isEmpty()").build())
            .addMethod(MethodSpec.methodBuilder("hasWarnings").addModifiers(Modifier.PUBLIC).returns(TypeName.BOOLEAN).addStatement("return !warnings.isEmpty()").build())
            .addMethod(MethodSpec.methodBuilder("getErrors").addModifiers(Modifier.PUBLIC).returns(errorList).addStatement("return errors").build())
            .addMethod(MethodSpec.methodBuilder("getWarnings").addModifiers(Modifier.PUBLIC).returns(warningList).addStatement("return warnings").build())
            .addMethod(MethodSpec.methodBuilder("throwIfInvalid")
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if (hasErrors())")
                .addStatement("throw new $T(errors)", validationException)
                .endControlFlow()
                .build())
            .build();

        JavaFile file = JavaFile.builder(pkg, type).skipJavaLangImports(true).indent("    ").build();
        return new CodeArtifact("ValidationResult", pkg, file.toString(), CodeArtifact.ArtifactType.CLASS);
    }

    private CodeArtifact generateGroupedValidationResult(String pkg) {
        ClassName validationError = ClassName.get(pkg, "ValidationError");
        ClassName validationException = ClassName.get(pkg, "ValidationException");
        TypeName errorList = ParameterizedTypeName.get(LIST, validationError);
        TypeName groupedMap = ParameterizedTypeName.get(MAP, ClassName.get(String.class), errorList);

        TypeSpec type = TypeSpec.classBuilder("GroupedValidationResult")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(FieldSpec.builder(groupedMap, "errorGroups", Modifier.PRIVATE, Modifier.FINAL).build())
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(groupedMap, "errorGroups")
                .addStatement("this.errorGroups = $T.copyOf(errorGroups)", java.util.Map.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("of")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(pkg, "GroupedValidationResult"))
                .addParameter(groupedMap, "errorGroups")
                .addStatement("return new GroupedValidationResult(errorGroups)")
                .build())
            .addMethod(MethodSpec.methodBuilder("isValid")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addStatement("return errorGroups.values().stream().allMatch($T::isEmpty)", List.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("hasErrors")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addStatement("return !isValid()")
                .build())
            .addMethod(MethodSpec.methodBuilder("getErrorGroups")
                .addModifiers(Modifier.PUBLIC)
                .returns(groupedMap)
                .addStatement("return errorGroups")
                .build())
            .addMethod(MethodSpec.methodBuilder("getErrorsForGroup")
                .addModifiers(Modifier.PUBLIC)
                .returns(errorList)
                .addParameter(String.class, "group")
                .addStatement("return errorGroups.getOrDefault(group, $T.of())", List.class)
                .build())
            .addMethod(MethodSpec.methodBuilder("getAllErrors")
                .addModifiers(Modifier.PUBLIC)
                .returns(errorList)
                .addStatement("$T<$T> all = new $T<>()", List.class, validationError, ArrayList.class)
                .addStatement("errorGroups.values().forEach(all::addAll)")
                .addStatement("return all")
                .build())
            .addMethod(MethodSpec.methodBuilder("throwIfInvalid")
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if (hasErrors())")
                .addStatement("throw new $T(getAllErrors())", validationException)
                .endControlFlow()
                .build())
            .build();

        JavaFile file = JavaFile.builder(pkg, type).skipJavaLangImports(true).indent("    ").build();
        return new CodeArtifact("GroupedValidationResult", pkg, file.toString(), CodeArtifact.ArtifactType.CLASS);
    }
}