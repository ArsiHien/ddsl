---
id: ddsl-pattern-003
category: DDD_PATTERN
subcategory: domain_event
complexity: intermediate
---
DDD Pattern: Domain Event Design
1. Name events in past tense: OrderPlaced, PaymentReceived
2. Events are immutable records of something that happened
3. Include the Aggregate ID that emitted the event
4. Include a timestamp field (usually DateTime)
5. Include only the data needed by consumers
6. Events should be emitted from Aggregate operations using 'emit event'
