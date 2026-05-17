---
id: ddsl-syntax-005
category: SYNTAX_RULE
subcategory: behavior
dsl_construct: Behavior
complexity: intermediate
version: 3.0
---

DDSL Behavior (Operations) Syntax - Parser-Compatible Format:

Behaviors define domain logic using parser-compatible natural-language clauses. The current recursive-descent parser requires `require that:` and `then:` to be followed by dash-prefixed list items.

Syntax inside 'operations { ... }':
```
when <action phrase> with <params>:
    require that:
        - <condition>
        - <condition>
    then:
        - <action description>
        - <action description>
    emit <EventName> with <properties>
    [return <expression>]
```

Parser Requirements:
1. Use `when ...:` with a colon, not brace-style `when ... { ... }`.
2. Use `require that:` with a colon.
3. Every precondition under `require that:` must start with `-`.
4. Use `then:` before state changes.
5. Every action under `then:` must start with `-`.
6. Use `emit EventName`, not `emit event EventName`.
7. Use temporal keyword `now`, not method-call syntax `now()`.

Action Types:
- set <field> to <value>
- calculate <field> as <expression>
- create <Object>
- add <item> to <collection>
- remove <item> from <collection>
- if <condition> then <action>
- for each <item> in <collection> <action>

Example - Parser-Compatible Format:
```ddsl
operations {
    when placing order with customer and items:
        require that:
            - customer is not empty
            - items is not empty
        then:
            - calculate total as sum of items price
            - set status to "PLACED"
            - set createdAt to now
        emit OrderPlaced with orderId and customer

    when confirming order:
        require that:
            - status is "PENDING"
        then:
            - set status to "CONFIRMED"
            - set confirmedAt to now
        emit OrderConfirmed with orderId

    when calculating discount with customerTier:
        then:
            - if customerTier is "GOLD" then set discount to 20
            - if customerTier is "SILVER" then set discount to 10
            - otherwise set discount to 0
            - calculate finalPrice as total minus discount
}
```

Invalid Legacy Styles:

Brace-style behavior is invalid:
```ddsl
when placing order with customer, items {
    require that customer is active
    require that items is not empty
    then set status to "PLACED"
    then calculate totalAmount as sum of item prices
    emit event OrderPlaced with orderId, customer
}
```

Natural no-bullet clauses are invalid for the current parser:
```ddsl
when placing order with customer and items:
    require that customer is not empty and items is not empty
    calculate totalAmount as sum of item prices
    set status to "PLACED"
    emit OrderPlaced with orderId and customer
```

Correct parser-compatible style:
```ddsl
when placing order with customer and items:
    require that:
        - customer is not empty
        - items is not empty
    then:
        - calculate totalAmount as sum of item prices
        - set status to "PLACED"
    emit OrderPlaced with orderId and customer
```

See Also:
- nl-to-ddsl/behavior-patterns.md for comprehensive behavior transformation guide
- nl-to-ddsl/complete-examples.md for real-world behavior examples
