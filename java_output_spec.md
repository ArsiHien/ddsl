# Output Java Code cho từng Logic/Syntax

---

## 1. Temporal Logic → Java Time API

### **Input DSL:**
```dsl
require that:
    - created at is before now
    - expires at is within next 7 days
    - last updated is more than 30 days ago
    - payment due date is between today and 30 days from now
```

### **Output Java:**

```java
// Guard clause cho "created at is before now"
if (!this.createdAt.isBefore(Instant.now())) {
    throw new DomainException("Created at must be before now");
}

// Guard clause cho "expires at is within next 7 days"
if (!this.expiresAt.isBefore(Instant.now().plus(7, ChronoUnit.DAYS))) {
    throw new DomainException("Expires at must be within next 7 days");
}

// Guard clause cho "last updated is more than 30 days ago"
if (!this.lastUpdated.isBefore(Instant.now().minus(30, ChronoUnit.DAYS))) {
    throw new DomainException("Last updated must be more than 30 days ago");
}

// Guard clause cho "payment due date is between today and 30 days from now"
LocalDate today = LocalDate.now();
LocalDate thirtyDaysFromNow = today.plusDays(30);
if (!(this.paymentDueDate.isAfter(today) && this.paymentDueDate.isBefore(thirtyDaysFromNow))) {
    throw new DomainException("Payment due date must be between today and 30 days from now");
}
```

### **Generated Helper Class:**

```java
package com.example.order.domain.temporal;

import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * Generated utility class for temporal operations
 */
public final class TemporalPredicates {
    
    // "X is before Y"
    public static boolean isBefore(Instant x, Instant y) {
        return x.isBefore(y);
    }
    
    // "X is after Y"
    public static boolean isAfter(Instant x, Instant y) {
        return x.isAfter(y);
    }
    
    // "X is more than N units ago"
    public static boolean isMoreThanAgo(Instant x, long amount, ChronoUnit unit) {
        return x.isBefore(Instant.now().minus(amount, unit));
    }
    
    // "X is less than N units ago"
    public static boolean isLessThanAgo(Instant x, long amount, ChronoUnit unit) {
        return x.isAfter(Instant.now().minus(amount, unit));
    }
    
    // "X is within next N units"
    public static boolean isWithinNext(Instant x, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant future = now.plus(amount, unit);
        return x.isAfter(now) && x.isBefore(future);
    }
    
    // "X is within last N units"
    public static boolean isWithinLast(Instant x, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant past = now.minus(amount, unit);
        return x.isAfter(past) && x.isBefore(now);
    }
    
    // "X is between A and B"
    public static boolean isBetween(Instant x, Instant start, Instant end) {
        return x.isAfter(start) && x.isBefore(end);
    }
}

// Usage in generated code
import static com.example.order.domain.temporal.TemporalPredicates.*;

if (!isMoreThanAgo(this.lastUpdated, 30, ChronoUnit.DAYS)) {
    throw new DomainException("Last updated must be more than 30 days ago");
}
```

---

## 2. State Machine → Enum + Validator + Lifecycle Hooks

### **Input DSL:**
```dsl
state machine for OrderStatus {
    states:
        - Pending (initial)
        - Confirmed
        - Shipped
        - Delivered (final)
        - Cancelled (final)
    
    transitions:
        - Pending -> Confirmed: when payment received
        - Confirmed -> Shipped: when inventory reserved
        - Shipped -> Delivered: always
        - [Pending, Confirmed] -> Cancelled: always
        - Delivered -> Cancelled: never
    
    guards:
        - cannot transition from Confirmed to Cancelled when shipped at is present
    
    on entry:
        - entering Confirmed:
            - record confirmed at as now
        - entering Cancelled:
            - record cancelled at as now
}
```

### **Output Java:**

#### **1. Enum với metadata**
```java
package com.example.order.domain.model;

public enum OrderStatus {
    PENDING(true, false),
    CONFIRMED(false, false),
    SHIPPED(false, false),
    DELIVERED(false, true),
    CANCELLED(false, true);
    
    private final boolean initial;
    private final boolean terminal;
    
    OrderStatus(boolean initial, boolean terminal) {
        this.initial = initial;
        this.terminal = terminal;
    }
    
    public boolean isInitial() {
        return initial;
    }
    
    public boolean isTerminal() {
        return terminal;
    }
}
```

#### **2. State Machine Validator**
```java
package com.example.order.domain.statemachine;

import com.example.order.domain.model.OrderStatus;
import java.util.*;

/**
 * Generated state machine validator for OrderStatus
 */
public class OrderStatusStateMachine {
    
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED, Set.of(),  // Terminal state
        OrderStatus.CANCELLED, Set.of()   // Terminal state
    );
    
    /**
     * Check if transition is allowed by state machine definition
     */
    public static boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        
        Set<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }
    
    /**
     * Validate transition with error message
     */
    public static void validateTransition(OrderStatus from, OrderStatus to) {
        if (!isTransitionAllowed(from, to)) {
            throw new IllegalStateTransitionException(
                String.format("Cannot transition from %s to %s", from, to)
            );
        }
    }
    
    /**
     * Get all possible next states from current state
     */
    public static Set<OrderStatus> getPossibleTransitions(OrderStatus from) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
    }
}
```

#### **3. Guard Checker**
```java
package com.example.order.domain.statemachine;

import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;

/**
 * Generated guard checker for OrderStatus state machine
 */
public class OrderStatusGuards {
    
    /**
     * Check all guards for transition
     */
    public static void checkGuards(Order order, OrderStatus from, OrderStatus to) {
        // Guard: cannot transition from Confirmed to Cancelled when shipped at is present
        if (from == OrderStatus.CONFIRMED && to == OrderStatus.CANCELLED) {
            if (order.getShippedAt() != null) {
                throw new StateTransitionGuardException(
                    "Cannot transition from Confirmed to Cancelled when shipped at is present"
                );
            }
        }
    }
}
```

#### **4. Lifecycle Hooks**
```java
package com.example.order.domain.statemachine;

import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import java.time.Instant;

/**
 * Generated lifecycle hooks for OrderStatus state machine
 */
public class OrderStatusLifecycle {
    
    /**
     * Execute on-entry actions for given state
     */
    public static void onEntry(Order order, OrderStatus state) {
        switch (state) {
            case CONFIRMED:
                // entering Confirmed: record confirmed at as now
                order.setConfirmedAt(Instant.now());
                break;
                
            case CANCELLED:
                // entering Cancelled: record cancelled at as now
                order.setCancelledAt(Instant.now());
                break;
                
            default:
                // No entry actions for other states
                break;
        }
    }
    
    /**
     * Execute on-exit actions for given state
     */
    public static void onExit(Order order, OrderStatus state) {
        // No exit actions defined in DSL
    }
}
```

#### **5. Integration trong Aggregate**
```java
package com.example.order.domain.model;

import com.example.order.domain.statemachine.*;
import java.time.Instant;

public class Order extends AggregateRoot<OrderId> {
    
    private OrderId id;
    private OrderStatus status;
    private Instant confirmedAt;
    private Instant shippedAt;
    private Instant cancelledAt;
    
    /**
     * Transition status with state machine validation
     */
    private void transitionStatusTo(OrderStatus newStatus) {
        // 1. Validate transition is allowed
        OrderStatusStateMachine.validateTransition(this.status, newStatus);
        
        // 2. Check guards
        OrderStatusGuards.checkGuards(this, this.status, newStatus);
        
        // 3. Execute on-exit hook
        OrderStatusLifecycle.onExit(this, this.status);
        
        // 4. Change state
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        
        // 5. Execute on-entry hook
        OrderStatusLifecycle.onEntry(this, newStatus);
    }
    
    /**
     * Behavior: confirm order
     */
    public void confirmOrder() {
        // Business logic preconditions
        if (this.paymentReceived == null) {
            throw new DomainException("Payment not received");
        }
        
        // Transition with state machine validation
        transitionStatusTo(OrderStatus.CONFIRMED);
        
        // Emit event
        OrderConfirmed event = new OrderConfirmed(this.id, this.confirmedAt, Instant.now());
        this.registerEvent(event);
    }
}
```

#### **6. Exception Classes**
```java
package com.example.order.domain.statemachine;

public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}

public class StateTransitionGuardException extends RuntimeException {
    public StateTransitionGuardException(String message) {
        super(message);
    }
}
```

---

## 3. Error Accumulation → ValidationResult Pattern

### **Input DSL:**
```dsl
when validating order placement with customerId and items:

    collect all errors:
        - items is not empty, otherwise "Cart cannot be empty"
        - all items have quantity greater than 0, otherwise "Invalid quantities"
        - customer exists in system, otherwise "Customer not found"
    
    fail if any errors
```

### **Output Java:**

#### **1. ValidationResult Class**
```java
package com.example.order.domain.validation;

import java.util.*;

/**
 * Result of validation containing collected errors and warnings
 */
public final class ValidationResult {
    
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    
    private ValidationResult(List<ValidationError> errors, List<ValidationWarning> warnings) {
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }
    
    public static ValidationResult success() {
        return new ValidationResult(List.of(), List.of());
    }
    
    public static ValidationResult ofErrors(List<ValidationError> errors) {
        return new ValidationResult(errors, List.of());
    }
    
    public static ValidationResult of(List<ValidationError> errors, List<ValidationWarning> warnings) {
        return new ValidationResult(errors, warnings);
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public List<ValidationWarning> getWarnings() {
        return warnings;
    }
    
    public void throwIfInvalid() {
        if (hasErrors()) {
            throw new ValidationException(errors);
        }
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult[errors=%d, warnings=%d]", 
            errors.size(), warnings.size());
    }
}
```

#### **2. ValidationError Class**
```java
package com.example.order.domain.validation;

/**
 * Represents a validation error
 */
public final class ValidationError {
    
    private final String field;
    private final String message;
    private final String code;
    
    public ValidationError(String message) {
        this(null, message, null);
    }
    
    public ValidationError(String field, String message) {
        this(field, message, null);
    }
    
    public ValidationError(String field, String message, String code) {
        this.field = field;
        this.message = message;
        this.code = code;
    }
    
    public String getField() {
        return field;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        if (field != null) {
            return String.format("[%s] %s", field, message);
        }
        return message;
    }
}
```

#### **3. ValidationException Class**
```java
package com.example.order.domain.validation;

import java.util.*;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RuntimeException {
    
    private final List<ValidationError> errors;
    
    public ValidationException(List<ValidationError> errors) {
        super(formatErrorMessage(errors));
        this.errors = List.copyOf(errors);
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    private static String formatErrorMessage(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return "Validation failed";
        }
        
        if (errors.size() == 1) {
            return errors.get(0).getMessage();
        }
        
        return String.format("Validation failed with %d errors: %s", 
            errors.size(),
            errors.stream()
                .map(ValidationError::getMessage)
                .limit(3)
                .collect(java.util.stream.Collectors.joining("; ")));
    }
}
```

#### **4. Generated Validation Method**
```java
package com.example.order.domain.model;

import com.example.order.domain.validation.*;
import java.util.*;

public class Order {
    
    /**
     * Generated validation method from DSL
     */
    private ValidationResult validateOrderPlacement(CustomerId customerId, List<CartItem> items) {
        List<ValidationError> errors = new ArrayList<>();
        
        // items is not empty
        if (items.isEmpty()) {
            errors.add(new ValidationError("items", "Cart cannot be empty"));
        }
        
        // all items have quantity greater than 0
        if (!items.stream().allMatch(item -> item.getQuantity() > 0)) {
            errors.add(new ValidationError("items", "Invalid quantities"));
        }
        
        // customer exists in system
        if (!customerService.exists(customerId)) {
            errors.add(new ValidationError("customerId", "Customer not found"));
        }
        
        return ValidationResult.ofErrors(errors);
    }
    
    /**
     * Business method using validation
     */
    public static Order placeOrder(
            CustomerId customerId,
            List<CartItem> items,
            CustomerService customerService) {
        
        Order order = new Order();
        
        // Perform validation and collect errors
        ValidationResult validation = order.validateOrderPlacement(customerId, items);
        
        // fail if any errors
        validation.throwIfInvalid();
        
        // Continue with business logic
        // ...
        
        return order;
    }
}
```

#### **5. Grouped Validation**

**Input DSL:**
```dsl
collect errors by group:
    customer errors:
        - customer ID is present, otherwise "Customer ID required"
        - customer exists, otherwise "Customer not found"
    
    items errors:
        - items is not empty, otherwise "Cart is empty"
        - all items have valid quantity, otherwise "Invalid quantities"
```

**Output Java:**
```java
package com.example.order.domain.validation;

import java.util.*;

/**
 * Grouped validation result
 */
public final class GroupedValidationResult {
    
    private final Map<String, List<ValidationError>> errorGroups;
    
    private GroupedValidationResult(Map<String, List<ValidationError>> errorGroups) {
        this.errorGroups = Map.copyOf(errorGroups);
    }
    
    public static GroupedValidationResult of(Map<String, List<ValidationError>> errorGroups) {
        return new GroupedValidationResult(errorGroups);
    }
    
    public boolean isValid() {
        return errorGroups.values().stream().allMatch(List::isEmpty);
    }
    
    public boolean hasErrors() {
        return !isValid();
    }
    
    public Map<String, List<ValidationError>> getErrorGroups() {
        return errorGroups;
    }
    
    public List<ValidationError> getErrorsForGroup(String group) {
        return errorGroups.getOrDefault(group, List.of());
    }
    
    public List<ValidationError> getAllErrors() {
        return errorGroups.values().stream()
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public void throwIfInvalid() {
        if (hasErrors()) {
            throw new ValidationException(getAllErrors());
        }
    }
}

// Generated validation method
private GroupedValidationResult validateOrderWithGroups(Order order) {
    Map<String, List<ValidationError>> errorGroups = new HashMap<>();
    
    // customer errors group
    List<ValidationError> customerErrors = new ArrayList<>();
    if (order.getCustomerId() == null) {
        customerErrors.add(new ValidationError("customerId", "Customer ID required"));
    }
    if (!customerService.exists(order.getCustomerId())) {
        customerErrors.add(new ValidationError("customerId", "Customer not found"));
    }
    errorGroups.put("customer", customerErrors);
    
    // items errors group
    List<ValidationError> itemsErrors = new ArrayList<>();
    if (order.getItems().isEmpty()) {
        itemsErrors.add(new ValidationError("items", "Cart is empty"));
    }
    if (!order.getItems().stream().allMatch(i -> i.getQuantity() > 0)) {
        itemsErrors.add(new ValidationError("items", "Invalid quantities"));
    }
    errorGroups.put("items", itemsErrors);
    
    return GroupedValidationResult.of(errorGroups);
}
```

---

## 4. Match Expression → Switch Expression/Statement


### **Match as Expression:**

**Input DSL:**
```dsl
given:
    - discount rate as match customer tier with:
        Gold:    0.15
        Silver:  0.10
        Bronze:  0.05
        default: 0.00
```

**Output Java:**
```java
BigDecimal discountRate = switch (customerTier) {
    case GOLD -> new BigDecimal("0.15");
    case SILVER -> new BigDecimal("0.10");
    case BRONZE -> new BigDecimal("0.05");
    default -> BigDecimal.ZERO;
};
```

### **Match với Guards (Java 21+ Pattern Matching):**

**Input DSL:**
```dsl
match customer tier with:
    Gold when total amount exceeds 50000000:
        - apply 20% discount
    Gold:
        - apply 15% discount
    Silver when total amount exceeds 20000000:
        - apply 12% discount
    Silver:
        - apply 10% discount
    default:
        - apply 5% discount
```

**Output Java (Java 21+):**
```java
switch (customerTier) {
    case GOLD when totalAmount.compareTo(new BigDecimal("50000000")) > 0 -> {
        this.discount = this.totalAmount.multiply(new BigDecimal("0.20"));
    }
    case GOLD -> {
        this.discount = this.totalAmount.multiply(new BigDecimal("0.15"));
    }
    case SILVER when totalAmount.compareTo(new BigDecimal("20000000")) > 0 -> {
        this.discount = this.totalAmount.multiply(new BigDecimal("0.12"));
    }
    case SILVER -> {
        this.discount = this.totalAmount.multiply(new BigDecimal("0.10"));
    }
    default -> {
        this.discount = this.totalAmount.multiply(new BigDecimal("0.05"));
    }
}
```

---

## 5. Specification in Conditions → Specification Pattern

### **Input DSL:**
```dsl
specifications {
    Specification ActiveCustomer {
        matches customers where:
            - customer status is Active
            - customer account is not suspended
    }
}

require that:
    - customer satisfies ActiveCustomer, otherwise "Customer not active"
```

### **Output Java:**

#### **1. Specification Interface**
```java
package com.example.order.domain.specification;

/**
 * Base specification interface
 */
public interface Specification<T> {
    
    boolean isSatisfiedBy(T candidate);
    
    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }
    
    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }
    
    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }
}
```

#### **2. Generated Specification Implementation**
```java
package com.example.order.domain.specification;

import com.example.order.domain.model.Customer;
import com.example.order.domain.model.CustomerStatus;

/**
 * Generated specification: ActiveCustomer
 */
public class ActiveCustomerSpecification implements Specification<Customer> {
    
    @Override
    public boolean isSatisfiedBy(Customer customer) {
        // customer status is Active
        boolean statusIsActive = customer.getStatus() == CustomerStatus.ACTIVE;
        
        // customer account is not suspended
        boolean accountNotSuspended = !customer.isAccountSuspended();
        
        return statusIsActive && accountNotSuspended;
    }
}
```

#### **3. Usage in Domain Logic**
```java
package com.example.order.domain.model;

import com.example.order.domain.specification.ActiveCustomerSpecification;

public class Order {
    
    private static final ActiveCustomerSpecification ACTIVE_CUSTOMER = 
        new ActiveCustomerSpecification();
    
    public static Order placeOrder(Customer customer, List<CartItem> items) {
        
        // customer satisfies ActiveCustomer
        if (!ACTIVE_CUSTOMER.isSatisfiedBy(customer)) {
            throw new DomainException("Customer not active");
        }
        
        // Continue with business logic
        // ...
    }
}
```

#### **4. Composite Specifications**

**Input DSL:**
```dsl
require that:
    - customer satisfies ActiveCustomer and VIPCustomer
    - order satisfies CancellableOrder or RefundableOrder
```

**Output Java:**
```java
// AND composition
if (!ACTIVE_CUSTOMER.and(VIP_CUSTOMER).isSatisfiedBy(customer)) {
    throw new DomainException("Customer must be active VIP");
}

// OR composition
if (!CANCELLABLE_ORDER.or(REFUNDABLE_ORDER).isSatisfiedBy(order)) {
    throw new DomainException("Order cannot be processed");
}
```

#### **5. Parameterized Specification**

**Input DSL:**
```dsl
Specification EligibleForDiscount given minimumAmount {
    matches orders where:
        - order total amount is at least minimumAmount
}

require that:
    - order satisfies EligibleForDiscount(50000)
```

**Output Java:**
```java
package com.example.order.domain.specification;

import com.example.order.domain.model.Order;
import java.math.BigDecimal;

/**
 * Parameterized specification: EligibleForDiscount
 */
public class EligibleForDiscountSpecification implements Specification<Order> {
    
    private final BigDecimal minimumAmount;
    
    public EligibleForDiscountSpecification(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }
    
    @Override
    public boolean isSatisfiedBy(Order order) {
        return order.getTotalAmount().compareTo(minimumAmount) >= 0;
    }
}

// Usage
if (!new EligibleForDiscountSpecification(new BigDecimal("50000")).isSatisfiedBy(order)) {
    throw new DomainException("Order not eligible for discount");
}
```

---

## 6. String Operations → String Methods + Regex

### **Input DSL:**
```dsl
require that:
    - email contains "@"
    - email matches "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    - name has length between 2 and 100
    - code starts with "PRE"
```

### **Output Java:**

```java
// email contains "@"
if (!email.contains("@")) {
    throw new DomainException("Email must contain @ symbol");
}

// email matches regex pattern
if (!email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
    throw new DomainException("Invalid email format");
}

// name has length between 2 and 100
if (name.length() < 2 || name.length() > 100) {
    throw new DomainException("Name must be between 2 and 100 characters");
}

// code starts with "PRE"
if (!code.startsWith("PRE")) {
    throw new DomainException("Code must start with PRE");
}
```

### **String Operations in Given:**

**Input DSL:**
```dsl
given:
    - normalized name as name trimmed
    - upper code as code converted to uppercase
    - short description as description truncated to 200 characters
```

**Output Java:**
```java
// normalized name as name trimmed
String normalizedName = name.trim();

// upper code as code converted to uppercase
String upperCode = code.toUpperCase();

// short description as description truncated to 200 characters
String shortDescription = description.substring(0, Math.min(description.length(), 200));
```

### **Generated String Validator Utility:**
```java
package com.example.order.domain.validation;

import java.util.regex.Pattern;

/**
 * Generated string validation utilities
 */
public final class StringValidators {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(0|\\+84)[0-9]{9,10}$"
    );
    
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    public static boolean hasLengthBetween(String str, int min, int max) {
        return str != null && str.length() >= min && str.length() <= max;
    }
    
    public static boolean matches(String str, String pattern) {
        return str != null && str.matches(pattern);
    }
}

// Usage
if (!StringValidators.isValidEmail(email)) {
    throw new DomainException("Invalid email format");
}
```

---

## 7. Nested Collection Operations → Stream API

### **Input DSL:**
```dsl
given:
    - confirmed total as sum of orders total amounts where status is Confirmed
    - all order items as all items across orders
    - revenue by category as sum of products price grouped by category
```

### **Output Java:**

```java
// sum of orders total amounts where status is Confirmed
BigDecimal confirmedTotal = orders.stream()
    .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
    .map(Order::getTotalAmount)
    .map(Money::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// all items across orders
List<OrderItem> allOrderItems = orders.stream()
    .flatMap(order -> order.getItems().stream())
    .collect(Collectors.toList());

// revenue by category grouped by category
Map<Category, BigDecimal> revenueByCategory = products.stream()
    .collect(Collectors.groupingBy(
        Product::getCategory,
        Collectors.mapping(
            product -> product.getPrice().getAmount(),
            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
        )
    ));
```

### **Complex Nested Example:**

**Input DSL:**
```dsl
given:
    - vip customer orders as orders where customer satisfies VIPCustomer
    - vip confirmed total as sum of vip customer orders total amounts 
                           where status is Confirmed
```

**Output Java:**
```java
// Filter by specification first
List<Order> vipCustomerOrders = orders.stream()
    .filter(order -> VIP_CUSTOMER.isSatisfiedBy(order.getCustomer()))
    .collect(Collectors.toList());

// Then aggregate with additional filter
BigDecimal vipConfirmedTotal = vipCustomerOrders.stream()
    .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
    .map(Order::getTotalAmount)
    .map(Money::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

---

## Tổng kết Mapping Table

| DSL Feature | Primary Java Output | Supporting Classes |
|-------------|--------------------|--------------------|
| **Temporal Logic** | `Instant.isBefore()`, `ChronoUnit` | `TemporalPredicates` utility |
| **State Machine** | Enum + Validator + Hooks | `StateMachine`, `Guards`, `Lifecycle` |
| **Error Accumulation** | `List<ValidationError>` | `ValidationResult`, `ValidationException` |
| **Match Expression** | `switch` expression/statement | None (pure Java) |
| **Specification** | `Specification<T>` interface | Concrete `XxxSpecification` classes |
| **String Operations** | String methods + `Pattern` | `StringValidators` utility |
| **Nested Collections** | Stream API (`flatMap`, `groupingBy`) | None (pure Java Streams) |

Tất cả output đều là **pure Java** không phụ thuộc framework!