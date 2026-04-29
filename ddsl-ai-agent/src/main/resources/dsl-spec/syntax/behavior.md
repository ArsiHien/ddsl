---
id: ddsl-syntax-005
category: SYNTAX_RULE
subcategory: behavior
dsl_construct: Behavior
complexity: intermediate
version: 3.0
---

DDSL Behavior (Operations) Syntax - Natural Format:

Behaviors define domain logic using natural-language clauses in a readable, flowing format without bullet points.

Syntax inside 'operations { ... }':
```
when <action phrase> with <params>:
    require that <condition> and <condition>
    <action description>
    <action description>
    emit <EventName> with <properties>
    [return <expression>]
```

Natural Format Principles:
1. Combine multiple requires with "and"
2. Write actions as readable sentences without bullet points
3. Flow naturally from requirement to action to event

Action Types:
- set <field> to <value>
- calculate <field> as <expression>
- create <Object>
- add <item> to <collection>
- remove <item> from <collection>
- if <condition> then <action>
- for each <item> in <collection> <action>

Example - Natural Format:
```ddsl
operations {
    when placing order with customer and items:
        require that customer is not empty and items is not empty
        calculate total as sum of items price
        set status to "PLACED"
        set createdAt to now
        emit OrderPlaced with orderId and customer

    when confirming order:
        require that status is "PENDING"
        set status to "CONFIRMED"
        set confirmedAt to now
        emit OrderConfirmed with orderId

    when calculating discount with customerTier:
        if customerTier is "GOLD" then set discount to 20
        if customerTier is "SILVER" then set discount to 10
        otherwise set discount to 0
        calculate finalPrice as total minus discount
}
```

Comparison with Old Bulleted Style:

Old Style:
```ddsl
when placing order with customer, items {
    require that customer is active
    require that items is not empty
    then set status to "PLACED"
    then calculate totalAmount as sum of item prices
    emit event OrderPlaced with orderId, customer
}
```

New Natural Style:
```ddsl
when placing order with customer and items:
    require that customer is not empty and items is not empty
    calculate totalAmount as sum of item prices
    set status to "PLACED"
    emit OrderPlaced with orderId and customer
```

See Also:
- nl-to-ddsl/behavior-patterns.md for comprehensive behavior transformation guide
- nl-to-ddsl/complete-examples.md for real-world behavior examples
