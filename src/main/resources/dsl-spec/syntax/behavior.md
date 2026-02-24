---
id: ddsl-syntax-005
category: SYNTAX_RULE
subcategory: behavior
dsl_construct: Behavior
complexity: intermediate
---
DDSL Behavior (Operations) Syntax:
Behaviors define domain logic using natural-language clauses.
Syntax inside 'operations { ... }':
when <action phrase> [with <params>] {
    require that <condition>
    given <precondition phrase>
    then <action>
    emit event <EventName> [with <properties>]
    return <expression>
}
Action types: set, calculate, create, add, remove, method call, if/then, for each
Example:
operations {
    when placing order with customer, items {
        require that customer is active
        require that items is not empty
        then set status to "PLACED"
        then calculate totalAmount as sum of item prices
        emit event OrderPlaced with orderId, customer
    }
}
