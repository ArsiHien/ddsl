package uet.ndh.ddsl.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uet.ndh.ddsl.ast.expr.StringCondition;
import uet.ndh.ddsl.ast.expr.TemporalComparison;
import uet.ndh.ddsl.ast.expr.TemporalRange;
import uet.ndh.ddsl.ast.expr.TemporalRelative;
import uet.ndh.ddsl.ast.expr.MethodCallExpr;
import uet.ndh.ddsl.parser.lexer.Scanner;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DdslParserExtendedExpressionTest {

    @Test
    @DisplayName("Scanner recognizes match/with keywords")
    void scannerRecognizesMatchKeywords() {
        Scanner scanner = new Scanner("match tier with: Gold: 0.15 default: 0.00");
        List<Token> tokens = scanner.scanTokens();

        assertEquals(TokenType.MATCH, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals(TokenType.WITH, tokens.get(2).getType());
        assertEquals(TokenType.COLON, tokens.get(3).getType());
    }

    @Test
    @DisplayName("Scanner recognizes state machine condition keywords")
    void scannerRecognizesStateMachineConditionKeywords() {
        Scanner scanner = new Scanner("state machine for status { transitions: - Pending -> Confirmed: always - Confirmed -> Pending: never }");
        List<Token> tokens = scanner.scanTokens();

        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.STATE));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.MACHINE));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ALWAYS));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.NEVER));
        assertEquals(4, tokens.stream().filter(t -> t.getType() == TokenType.DASH).count());
        assertEquals(2, tokens.stream().filter(t -> t.getType() == TokenType.RIGHT_ANGLE).count());
    }

    @Test
    @DisplayName("Scanner treats email/phone as identifiers (not reserved keywords)")
    void scannerTreatsEmailAndPhoneAsIdentifiers() {
        Scanner scanner = new Scanner("customerEmail has valid email format and customerPhone has valid phone number format");
        List<Token> tokens = scanner.scanTokens();

        assertTrue(tokens.stream().anyMatch(t -> t.getLexeme().equals("email") && t.getType() == TokenType.IDENTIFIER));
        assertTrue(tokens.stream().anyMatch(t -> t.getLexeme().equals("phone") && t.getType() == TokenType.IDENTIFIER));
        assertTrue(tokens.stream().anyMatch(t -> t.getLexeme().equals("number") && t.getType() == TokenType.IDENTIFIER));
    }

    @Test
    @DisplayName("Parser supports standalone state machine and collect errors clauses")
    void parserSupportsStateMachineAndCollectErrors() throws ParseException {
        String ddsl = """
            BoundedContext Orders {
                domain {
                    state machine for OrderStatus {
                        states:
                            - Pending (initial)
                            - Confirmed
                            - Cancelled (final)
                        transitions:
                            - Pending -> Confirmed: always
                            - Confirmed -> Cancelled: when payment failed
                        guards:
                            - cannot transition from Confirmed to Pending when payment captured
                        on entry:
                            - entering Confirmed:
                                - record confirmed at as now
                        on exit:
                            - leaving Pending:
                                - record exited pending at as now
                    }

                    Aggregate Order {
                        id: UUID @identity
                        status: OrderStatus

                        when validating order with orderId:
                            collect all errors:
                                - orderId is present, otherwise "Order id required"
                            fail if any errors
                        then:
                            - set status to Confirmed
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<test>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        assertEquals(1, model.boundedContexts().size());
        assertEquals(1, model.boundedContexts().getFirst().stateMachines().size());
        assertFalse(model.boundedContexts().getFirst().stateMachines().getFirst().onEntryRules().isEmpty());
        assertFalse(model.boundedContexts().getFirst().stateMachines().getFirst().onExitRules().isEmpty());
        assertNotNull(model.boundedContexts().getFirst().aggregates().getFirst().behaviors().getFirst().errorAccumulationClause());
    }

    @Test
    @DisplayName("Parser supports keyword-like field names, bracket match patterns, and natural language spec predicates")
    void parserSupportsExtendedNaturalLanguageForms() throws ParseException {
        String ddsl = """
            BoundedContext SpecForms {
                domain {
                    Aggregate Order {
                        items: List<String>

                        operations {
                            when validating order with customerTier:
                            given:
                                - discountRate as match customerTier with:
                                    [BRONZE, BASIC]: 0.05
                                    default: 0.00
                            then:
                                - set items to items
                        }
                    }
                }

                specifications {
                    Specification ActiveCustomer {
                        matches customers where:
                            - customer status is Active
                            - customer account is not suspended
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<extended-natural-language>");
        Scanner scanner = new Scanner(ddsl);
        List<Token> tokens = scanner.scanTokens();
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ITEMS), "Expected ITEMS token");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.LIST_TYPE), "Expected LIST_TYPE token");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.LEFT_ANGLE), "Expected LEFT_ANGLE token");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.RIGHT_ANGLE), "Expected RIGHT_ANGLE token");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        assertEquals(1, model.boundedContexts().size());
        assertEquals(1, model.boundedContexts().getFirst().aggregates().size());
        assertEquals(1, model.boundedContexts().getFirst().specifications().size());
    }

    @Test
    @DisplayName("Parser supports match expression with multiple plain cases")
    void parserSupportsMatchWithMultiplePlainCases() throws ParseException {
        String ddsl = """
            BoundedContext MatchCases {
                domain {
                    Aggregate Order {
                        customerTier: String
                        status: String

                        operations {
                            when validating order with customerTier:
                            given:
                                - discountRate as match customerTier with:
                                    GOLD when customerTier is "GOLD": 0.20
                                    GOLD: 0.15
                                    SILVER: 0.10
                                    default: 0.00
                            then:
                                - set status to "CONFIRMED"
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<match-multi-cases>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        assertEquals(1, model.boundedContexts().size());
        assertEquals(1, model.boundedContexts().getFirst().aggregates().size());
        assertEquals(1, model.boundedContexts().getFirst().aggregates().getFirst().behaviors().size());
    }

    @Test
    @DisplayName("Parser structures temporal and string require conditions")
    void parserStructuresTemporalAndStringConditions() throws ParseException {
        String ddsl = """
            BoundedContext TemporalString {
                domain {
                    Aggregate Order {
                        orderId: UUID @identity
                        createdAt: DateTime
                        lastUpdated: DateTime
                        dueDate: DateTime
                        customerEmail: String

                        operations {
                            when validating order:
                            require that:
                                - createdAt is before now
                                - lastUpdated is more than 30 days ago
                                - dueDate is between today and 30 days from now
                                - customerEmail has valid email format
                                - customerEmail has length between 5 and 100
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<temporal-string>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        var conditions = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .requireClause().conditions();

        assertInstanceOf(TemporalComparison.class, conditions.get(0).condition().leftExpression());
        assertInstanceOf(TemporalRelative.class, conditions.get(1).condition().leftExpression());
        assertInstanceOf(TemporalRange.class, conditions.get(2).condition().leftExpression());
        assertInstanceOf(StringCondition.class, conditions.get(3).condition().leftExpression());
        assertInstanceOf(StringCondition.class, conditions.get(4).condition().leftExpression());
    }

    @Test
    @DisplayName("Parser supports first-class enum declarations")
    void parserSupportsEnumDeclarations() throws ParseException {
        String ddsl = """
            BoundedContext EnumCtx {
                domain {
                    Enum CustomerTier {
                        GOLD
                        SILVER
                        BRONZE
                    }

                    Aggregate Order {
                        orderId: UUID @identity
                        tier: CustomerTier
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<enum-declaration>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());
        var context = model.boundedContexts().getFirst();
        assertEquals(1, context.enums().size(), "Expected one enum declaration");
        assertEquals("CustomerTier", context.enums().getFirst().name());
        assertEquals(3, context.enums().getFirst().values().size());
    }

    @Test
    @DisplayName("Parser supports natural then-clause method calls")
    void parserSupportsNaturalThenMethodCalls() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
                    Aggregate Order {
                        orderId: UUID @identity
                        customerEmail: String

                        operations {
                            when notifying customer:
                            then:
                                - notify customerEmail
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<natural-call>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertNotNull(statement.expression());
        assertInstanceOf(MethodCallExpr.class, statement.expression());

        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertEquals("notify", call.methodName());
        assertEquals(1, call.arguments().size());
    }

    @Test
    @DisplayName("Parser normalizes save-to-repository into method call expression")
    void parserNormalizesSaveStatementToMethodCall() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
                    Aggregate Order {
                        orderId: UUID @identity

                        operations {
                            when persisting order with order:
                            then:
                                - save order to orderRepository
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<save-call>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertInstanceOf(MethodCallExpr.class, statement.expression());
        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertEquals("save", call.methodName());
        assertTrue(call.hasReceiver(), "Save call should have repository receiver");
        assertEquals(1, call.arguments().size(), "Save call should pass entity argument");
    }

    @Test
    @DisplayName("Parser supports explicit call syntax for long method names")
    void parserSupportsExplicitCallSyntax() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
                    Aggregate Order {
                        orderId: UUID @identity
                        customerEmail: String

                        operations {
                            when processing order:
                            then:
                                - call sendOrderConfirmationEmail(customerEmail)
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<explicit-call>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertInstanceOf(MethodCallExpr.class, statement.expression());
        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertEquals("sendOrderConfirmationEmail", call.methodName());
        assertEquals(1, call.arguments().size());
    }

    @Test
    @DisplayName("Parser supports explicit receiver call syntax")
    void parserSupportsExplicitReceiverCallSyntax() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
                    Aggregate Order {
                        orderId: UUID @identity

                        operations {
                            when persisting order with order:
                            then:
                                - call orderRepository.save(order)
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<explicit-receiver-call>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertInstanceOf(MethodCallExpr.class, statement.expression());
        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertTrue(call.hasReceiver());
        assertEquals("save", call.methodName());
        assertEquals(1, call.arguments().size());
    }

    @Test
    @DisplayName("Parser supports execute syntax with phrase method names")
    void parserSupportsExecutePhraseSyntax() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
                    Aggregate Reservation {
                        reservationId: UUID @identity

                        operations {
                            when placing reservation:
                            then:
                                - execute apply seasonal discount
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<execute-phrase>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertInstanceOf(MethodCallExpr.class, statement.expression());
        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertEquals("applySeasonalDiscount", call.methodName());
    }

    @Test
    @DisplayName("Parser supports execute with arguments")
    void parserSupportsExecuteWithArguments() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
                    Aggregate Reservation {
                        reservationId: UUID @identity
                        totalCost: Decimal

                        operations {
                            when placing reservation:
                            then:
                                - execute calculate tax with totalCost and \"VAT_10\"
                        }
                    }
                }
            }
            """;

        DdslParser parser = new DdslParser(ddsl, "<execute-with-args>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertInstanceOf(MethodCallExpr.class, statement.expression());
        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertEquals("calculateTax", call.methodName());
        assertEquals(2, call.arguments().size());
    }

    @Test
    @DisplayName("Parser supports external call syntax with service and action phrase")
    void parserSupportsExternalCallPhraseSyntax() throws ParseException {
        String ddsl = """
            BoundedContext Calls {
                domain {
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

        DdslParser parser = new DdslParser(ddsl, "<external-call-phrase>");
        var model = parser.parse();

        assertFalse(parser.hasErrors(), "Parser errors: " + parser.getErrors());

        var statement = model.boundedContexts().getFirst()
            .aggregates().getFirst()
            .behaviors().getFirst()
            .thenClauses().getFirst()
            .statements().getFirst();

        assertInstanceOf(MethodCallExpr.class, statement.expression());
        MethodCallExpr call = (MethodCallExpr) statement.expression();
        assertTrue(call.hasReceiver());
        assertEquals("processPayment", call.methodName());
        assertEquals(2, call.arguments().size());
    }
}
