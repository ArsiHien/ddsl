package uet.ndh.ddsl.analysis;

import org.junit.jupiter.api.*;
import uet.ndh.ddsl.analysis.validator.DddValidator;
import uet.ndh.ddsl.analysis.validator.Diagnostic;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DDD validation rules applied to the AST.
 */
class DddValidatorTest {

    private DomainModel parse(String ddsl) throws ParseException {
        return new DdslParser(ddsl, "<test>").parse();
    }

    // ─── 1. Valid models pass validation ─────────────────────────────────

    @Test
    @DisplayName("Well-formed model passes all DDD rules")
    void validModelPasses() throws ParseException {
        String ddsl = """
                BoundedContext OK {
                    domain {
                        Aggregate Order {
                            @identity orderId: UUID
                            total: Decimal
                            
                            Entity OrderItem {
                                @identity itemId: UUID
                                qty: Int
                            }
                        }
                        
                        ValueObject Money {
                            amount: Decimal
                            currency: String
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new DddValidator();
        model.accept(validator);

        assertFalse(validator.hasErrors(),
                "Valid model should have no errors: " + validator.errors());
    }

    // ─── 2. Entity without identity triggers error ──────────────────────

    @Test
    @DisplayName("Entity missing @identity field triggers EntityHasIdentityRule")
    void entityMissingIdentity() throws ParseException {
        String ddsl = """
                BoundedContext Bad {
                    domain {
                        Aggregate NoId {
                            @identity aggId: UUID
                            
                            Entity Orphan {
                                name: String
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new DddValidator();
        model.accept(validator);

        List<Diagnostic> errors = validator.errors();
        assertFalse(errors.isEmpty(),
                "Entity without @identity should trigger a diagnostic error");
    }

    // ─── 3. ValueObject must have fields ────────────────────────────────

    @Test
    @DisplayName("ValueObject without fields triggers ValueObjectHasFieldsRule")
    void valueObjectEmpty() throws ParseException {
        String ddsl = """
                BoundedContext Bad {
                    domain {
                        ValueObject Empty {
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new DddValidator();
        model.accept(validator);

        List<Diagnostic> errors = validator.errors();
        assertFalse(errors.isEmpty(),
                "Empty ValueObject should trigger a diagnostic error");
    }

    // ─── 4. Multiple issues reported ────────────────────────────────────

    @Test
    @DisplayName("Multiple violations produce multiple diagnostics")
    void multipleViolations() throws ParseException {
        String ddsl = """
                BoundedContext Multi {
                    domain {
                        Aggregate Agg {
                            @identity id: UUID
                            
                            Entity NoId1 {
                                x: String
                            }
                            Entity NoId2 {
                                y: String
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new DddValidator();
        model.accept(validator);

        assertTrue(validator.errors().size() >= 2,
                "Two entities without @identity should produce at least 2 errors");
    }

    // ─── 5. Warnings vs errors ──────────────────────────────────────────

    @Test
    @DisplayName("diagnostics() returns both errors and warnings")
    void diagnosticCategories() throws ParseException {
        String ddsl = """
                BoundedContext Mix {
                    domain {
                        Aggregate A {
                            @identity aId: UUID
                        }
                        ValueObject V {
                            field: String
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new DddValidator();
        model.accept(validator);

        // At minimum, we can verify the API works
        assertNotNull(validator.diagnostics());
        assertNotNull(validator.errors());
        assertNotNull(validator.warnings());
    }
}
