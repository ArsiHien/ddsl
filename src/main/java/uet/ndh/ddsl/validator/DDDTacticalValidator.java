package uet.ndh.ddsl.validator;

import lombok.Data;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.entity.EntityMustHaveIdentityRule;
import uet.ndh.ddsl.validator.rules.entity.EntityEqualsByIdentityRule;
import uet.ndh.ddsl.validator.rules.entity.EntityIdentityImmutableRule;
import uet.ndh.ddsl.validator.rules.valueobject.ValueObjectImmutableRule;
import uet.ndh.ddsl.validator.rules.valueobject.ValueObjectNoIdentityRule;
import uet.ndh.ddsl.validator.rules.aggregate.AggregateHasOneRootRule;
import uet.ndh.ddsl.validator.rules.aggregate.AggregateReferenceByIdOnlyRule;
import uet.ndh.ddsl.validator.rules.repository.RepositoryPerAggregateRootRule;
import uet.ndh.ddsl.validator.rules.application.ApplicationServiceNoBusinessLogicRule;
import uet.ndh.ddsl.validator.rules.event.DomainEventImmutableRule;
import uet.ndh.ddsl.validator.rules.event.DomainEventPastTenseNamingRule;
import uet.ndh.ddsl.validator.rules.event.DomainEventRequiredFieldsRule;
import uet.ndh.ddsl.validator.rules.aggregate.AggregateSizeLimitRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive DDD Tactical Design Validator.
 * Validates domain models against all DDD tactical design patterns.
 */
@Data
public class DDDTacticalValidator {

    private final List<DDDValidationRule> rules;

    public DDDTacticalValidator() {
        this.rules = Arrays.asList(
            // Entity Rules (1.x)
            new EntityMustHaveIdentityRule(),
            new EntityEqualsByIdentityRule(),
            new EntityIdentityImmutableRule(),

            // Value Object Rules (2.x)
            new ValueObjectImmutableRule(),
            new ValueObjectNoIdentityRule(),

            // Aggregate Rules (3.x)
            new AggregateHasOneRootRule(),
            new AggregateReferenceByIdOnlyRule(),
            new AggregateSizeLimitRule(),

            // Domain Event Rules (4.x)
            new DomainEventImmutableRule(),
            new DomainEventPastTenseNamingRule(),
            new DomainEventRequiredFieldsRule(),

            // Repository Rules (5.x)
            new RepositoryPerAggregateRootRule(),

            // Application Service Rules (8.x)
            new ApplicationServiceNoBusinessLogicRule()

            // TODO: Add more rules for:
            // - Domain Services (6.x)
            // - Factories (7.x)
            // - Invariants (9.x)
            // - Specifications (10.x)
            // - Package/Module structure (11.x)
            // - Aggregate size limits
            // - Cross-aggregate transaction boundaries
            // - Domain events timestamp and aggregate ID requirements
            // - Repository method naming conventions
            // - Domain service statelessness
        );
    }

    /**
     * Validate domain model against all DDD tactical design rules.
     * @param model The domain model to validate
     * @return List of validation errors found
     */
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> allErrors = new ArrayList<>();

        System.out.println("🔍 Running DDD Tactical Design Validation...");
        System.out.println("📋 Checking " + rules.size() + " validation rules\n");

        for (DDDValidationRule rule : rules) {
            try {
                System.out.print("  ✓ " + rule.getRuleCode() + ": " + rule.getRuleName() + "... ");
                List<DDDValidationError> ruleErrors = rule.validate(model);

                if (ruleErrors.isEmpty()) {
                    System.out.println("✅ PASS");
                } else {
                    System.out.println("❌ " + ruleErrors.size() + " issue(s) found");
                    allErrors.addAll(ruleErrors);
                }
            } catch (Exception e) {
                // Log the exception and continue with other rules
                System.out.println("💥 ERROR: " + e.getMessage());
                System.err.println("Error executing rule " + rule.getRuleCode() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println();
        return allErrors;
    }

    /**
     * Add a custom validation rule.
     * @param rule The rule to add
     */
    public void addRule(DDDValidationRule rule) {
        rules.add(rule);
    }

    /**
     * Get summary of validation results.
     * @param errors List of validation errors
     * @return Validation summary
     */
    public DDDValidationSummary getSummary(List<DDDValidationError> errors) {
        return new DDDValidationSummary(errors);
    }
}
