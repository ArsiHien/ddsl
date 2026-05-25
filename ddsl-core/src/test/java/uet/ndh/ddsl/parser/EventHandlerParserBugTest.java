package uet.ndh.ddsl.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import uet.ndh.ddsl.ast.model.DomainModel;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for parser infinite loop bug fix in event-handlers section.
 */
class EventHandlerParserBugTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMalformedEventHandlerDoesNotCauseInfiniteLoop() {
        // This malformed DSL has an event handler missing the 'EventHandler' keyword
        // Before the fix, this would cause an infinite loop and OOM
        String malformedDsl = """
BoundedContext TestEventHandlerBug {
    events {
        DomainEvent TestEvent {
            id: UUID
        }
    }

    domain {
        Aggregate TestAggregate {
            id: UUID @identity
        }
    }

    event-handlers {
        BadHandler for TestEvent {
            when testing:
                then:
                    - set done to true
        }
    }
}
""";

        DdslParser parser = new DdslParser(malformedDsl, "test");

        // parse() throws ParseException when there are errors
        ParseException exception = assertThrows(ParseException.class, () -> {
            parser.parse();
        });

        List<DdslParser.ParseError> errors = exception.getErrors();

        // Should have errors but should NOT hang or OOM
        assertFalse(errors.isEmpty(), "Expected parse errors for malformed event handler");

        // Verify we got the expected error about 'EventHandler' keyword
        boolean hasExpectedError = errors.stream()
            .anyMatch(e -> e.message().contains("Expected 'EventHandler'"));
        assertTrue(hasExpectedError, "Should have error about missing 'EventHandler' keyword");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testValidEventHandlerParsesCorrectly() {
        String validDsl = """
BoundedContext TestEventHandlerValid {
    events {
        DomainEvent OrderPlaced {
            orderId: UUID
        }
    }

    domain {
        Aggregate Order {
            orderId: UUID @identity
        }
    }

    event-handlers {
        EventHandler OrderHandler for OrderPlaced {
            when handling order:
                then:
                    - set processed to true
        }
    }
}
""";

        DdslParser parser = new DdslParser(validDsl, "test");

        DomainModel model = assertDoesNotThrow(() -> parser.parse());

        assertNotNull(model);
        assertEquals(1, model.boundedContexts().size());
        assertFalse(model.boundedContexts().get(0).eventHandlers().isEmpty());
        assertEquals(1, model.boundedContexts().get(0).eventHandlers().size());
    }
}
