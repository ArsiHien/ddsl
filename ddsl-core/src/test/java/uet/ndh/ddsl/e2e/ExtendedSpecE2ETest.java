package uet.ndh.ddsl.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.ProjectWriter;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedSpecE2ETest {

    private static final String SAMPLE = "samples/spec-e2e.ddsl";
    private static final Path OUTPUT_DIR = Path.of("build", "generated-test", "spec-e2e", "src", "main", "java");

    @Test
    @DisplayName("E2E spec sample parses and generates Java 21+ style helpers/code")
    void specE2EParseAndCodegen() throws IOException, ParseException {
        String ddsl = Files.readString(Path.of(SAMPLE));

        DdslParser parser = new DdslParser(ddsl, SAMPLE);
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        assertEquals(1, model.boundedContexts().size());
        assertFalse(model.boundedContexts().getFirst().stateMachines().isEmpty(), "State machine should be parsed");

        PoetModule poet = new PoetModule("com.example.spec");
        List<CodeArtifact> artifacts = poet.generateFromModel(model);

        ProjectWriter writer = new ProjectWriter();
        ProjectWriter.WriteResult writeResult = writer.writeAll(artifacts, OUTPUT_DIR);

        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("TemporalPredicates")),
            "Temporal helper should be generated");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("ValidationResult")),
            "ValidationResult helper should be generated");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("ValidationException")),
            "ValidationException helper should be generated");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("IllegalStateTransitionException")),
            "State-machine exception helper should be generated");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("StateTransitionGuardException")),
            "Guard exception helper should be generated");

        CodeArtifact order = artifacts.stream()
            .filter(a -> a.typeName().equals("Order"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Order aggregate should be generated"));

        String orderSrc = order.sourceCode();

        boolean hasJava21GuardedSwitchCase = orderSrc.contains("case \"GOLD\" when")
            && orderSrc.contains("this.totalAmount > 1000");

        assertTrue(hasJava21GuardedSwitchCase,
            "Generated code should contain Java 21 guarded switch case for match expression");

        assertTrue(orderSrc.contains("TemporalPredicates.isBefore(createdAt, Instant.now())")
            || orderSrc.contains("TemporalPredicates.isBefore(this.createdAt, Instant.now())"),
            "Temporal comparison should be translated to TemporalPredicates helper");
        assertTrue(orderSrc.contains("TemporalPredicates.isMoreThanAgo(lastUpdated, 30, ChronoUnit.DAYS)")
            || orderSrc.contains("TemporalPredicates.isMoreThanAgo(this.lastUpdated, 30, ChronoUnit.DAYS)"),
            "Relative temporal condition should be translated to helper");
        assertTrue(orderSrc.contains("TemporalPredicates.isWithinNext(dueDate, 30, ChronoUnit.DAYS)")
            || orderSrc.contains("TemporalPredicates.isWithinNext(this.dueDate, 30, ChronoUnit.DAYS)"),
            "Between today and N days from now should be lowered to WITHIN_NEXT helper");
        assertTrue(orderSrc.contains("customerEmail.matches("),
            "Regex string condition should be translated");
        assertTrue(orderSrc.contains("customerEmail.length() >= 5 && customerEmail.length() <= 100"),
            "Length-between string condition should be translated");
        assertTrue(orderSrc.contains("ValidationResult.ofErrors(errors)"),
            "Error accumulation should use ValidationResult factory API");
        assertFalse(orderSrc.contains("/* "),
            "Generated Order should not contain placeholder comments for supported expressions");

        assertTrue(writeResult.isSuccess(), "Generated files should be written successfully");
        assertTrue(artifacts.stream().anyMatch(a -> Files.exists(OUTPUT_DIR.resolve(a.relativePath()))),
            "At least one generated file should exist in output directory");
    }
}
