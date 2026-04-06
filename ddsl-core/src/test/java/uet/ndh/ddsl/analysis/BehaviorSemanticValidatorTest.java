package uet.ndh.ddsl.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uet.ndh.ddsl.analysis.validator.BehaviorSemanticValidator;
import uet.ndh.ddsl.analysis.validator.Diagnostic;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorSemanticValidatorTest {

    private DomainModel parse(String ddsl) throws ParseException {
        DdslParser parser = new DdslParser(ddsl, "<test>");
        DomainModel model = parser.parse();
        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        return model;
    }

    @Test
    @DisplayName("Undeclared assignment target in DomainService behavior emits SEM001")
    void undeclaredResultInDomainServiceBehavior() throws ParseException {
        String ddsl = """
                BoundedContext HotelBooking {
                    domain {
                        ValueObject DateRange {
                            startDate: DateTime
                            endDate: DateTime
                        }

                        DomainService AvailabilityService {
                            when checking availability with roomType as String and dateRange as DateRange:
                            then:
                                - set result to roomType
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        List<Diagnostic> errors = validator.errors();
        assertEquals(1, errors.size(), "Expected exactly one undeclared identifier error");
        assertEquals("SEM001", errors.getFirst().ruleId());
        assertTrue(errors.getFirst().message().contains("Identifier 'result' not found in scope of DomainService 'AvailabilityService'"));
    }

    @Test
    @DisplayName("Declared aggregate field assignment target is accepted")
    void declaredAggregateFieldTargetIsValid() throws ParseException {
        String ddsl = """
                BoundedContext Orders {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            status: String

                            operations {
                                when placing order:
                                then:
                                    - set status to "PENDING"
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertFalse(validator.hasErrors(), "No undeclared-target errors expected for declared field assignment");
    }

    @Test
    @DisplayName("Undefined implicit behavior parameters emit identifier diagnostics")
    void undefinedImplicitBehaviorParameters() throws ParseException {
        String ddsl = """
                BoundedContext SpecE2E {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerEmail: String
                            customerPhone: String

                            operations {
                                when validating order with customerEmail and customerPhone and orders and products:
                                then:
                                    - set customerEmail to customerEmail
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        List<Diagnostic> errors = validator.errors();
        assertTrue(errors.stream().anyMatch(e -> e.ruleId().equals("SEM108") && e.message().contains("orders")),
                "Expected undefined identifier diagnostic for 'orders'");
        assertTrue(errors.stream().anyMatch(e -> e.ruleId().equals("SEM108") && e.message().contains("products")),
                "Expected undefined identifier diagnostic for 'products'");
    }

    @Test
    @DisplayName("Explicitly typed behavior parameters are treated as declared")
    void explicitlyTypedBehaviorParametersAreAllowed() throws ParseException {
        String ddsl = """
                BoundedContext Services {
                    domain {
                        DomainService AvailabilityService {
                            when checking availability with roomType as String and dateRange as DateRange:
                            then:
                                - set result to roomType
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertTrue(validator.errors().stream().noneMatch(e -> e.ruleId().equals("SEM108") && e.message().contains("roomType")),
                "Explicitly typed parameter 'roomType' should be treated as declared");
    }

    @Test
    @DisplayName("Unknown local method call emits SEM202")
    void unknownLocalMethodCallEmitsSemanticError() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerEmail: String

                            operations {
                                when placing order:
                                then:
                                    - call sendOrderConfirmationEmail(customerEmail)
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertTrue(validator.errors().stream().anyMatch(e -> e.ruleId().equals("SEM202")
                        && e.message().contains("sendOrderConfirmationEmail")),
                "Expected unresolved method semantic error for local call");
    }

    @Test
    @DisplayName("Existing local behavior method call is accepted")
    void existingLocalMethodCallIsAccepted() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerEmail: String

                            operations {
                                when send order confirmation email with customerEmail:
                                then:
                                    - set customerEmail to customerEmail

                                when placing order:
                                then:
                                    - call sendOrderConfirmationEmail(customerEmail)
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertTrue(validator.errors().stream().noneMatch(e -> e.ruleId().equals("SEM202")),
                "Existing behavior call should not emit SEM202");
    }

    @Test
    @DisplayName("Method arity mismatch emits SEM204")
    void methodArityMismatchEmitsSemanticError() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerEmail: String

                            operations {
                                when send order confirmation email with customerEmail:
                                then:
                                    - set customerEmail to customerEmail

                                when placing order:
                                then:
                                    - call sendOrderConfirmationEmail(customerEmail, customerEmail)
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertTrue(validator.errors().stream().anyMatch(e -> e.ruleId().equals("SEM204")
                        && e.message().contains("sendOrderConfirmationEmail")),
                "Expected arity mismatch semantic error");
    }

    @Test
    @DisplayName("External call to existing domain service action is accepted")
    void externalCallToExistingServiceActionIsAccepted() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        DomainService PaymentService {
                            when process payment with amount and guestId:
                            then:
                                - set result to amount
                        }

                        Aggregate Reservation {
                            reservationId: UUID @identity
                            amount: Decimal
                            guestId: UUID

                            operations {
                                when placing reservation:
                                then:
                                    - call PaymentService to process payment with amount, guestId
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertTrue(validator.errors().stream().noneMatch(e -> e.ruleId().equals("SEM202")),
                "Existing service action call should not emit SEM202");
    }

    @Test
    @DisplayName("External call to missing action emits SEM202")
    void externalCallToMissingServiceActionEmitsSemanticError() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        DomainService PaymentService {
                            when process payment with amount and guestId:
                            then:
                                - set result to amount
                        }

                        Aggregate Reservation {
                            reservationId: UUID @identity
                            amount: Decimal
                            guestId: UUID

                            operations {
                                when placing reservation:
                                then:
                                    - call PaymentService to refund payment with amount, guestId
                            }
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var validator = new BehaviorSemanticValidator();
        model.accept(validator);

        assertTrue(validator.errors().stream().anyMatch(e -> e.ruleId().equals("SEM202")
                        && e.message().contains("refundPayment")),
                "Missing service action should emit SEM202");
    }
}
