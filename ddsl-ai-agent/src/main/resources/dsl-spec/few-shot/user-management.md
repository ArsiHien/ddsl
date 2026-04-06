---
id: ddsl-fewshot-004
category: FEW_SHOT
subcategory: informal_mixed
language: en
complexity: intermediate
---
This example shows how to handle a mix of very informal descriptions — compressed behaviors, loose field definitions, casual event and repository syntax.

User input (mix of casual English and pseudo-DDSL):
```
BoundedContext UserManagement {
    domain {
        Aggregate User {
            user id is UUID, this is the identity
            name is text, required, max 100 chars
            email is text, required, must be valid email
            password is text, required, at least 8 characters
            registered at is a timestamp
            active is a boolean, default true

            operations {
                registering a new user with name, email, password {
                    email and password cannot be empty, activate the user, record registration time, notify with UserRegistered event including userId and email
                }

                deactivating user {
                    user must be currently active, set active to false, send UserDeactivated event with userId
                }

                changing password with oldPassword, newPassword {
                    old password must match current, new password must be at least 8 chars, update password, fire PasswordChanged event
                }
            }
        }
    }
    events {
        DomainEvent UserRegistered {
            user id is UUID
            email is text
            registered at is timestamp
        }
        DomainEvent UserDeactivated {
            user id is UUID
            deactivated at is timestamp
        }
        DomainEvent PasswordChanged {
            user id is UUID
            changed at is timestamp
        }
    }
    repositories {
        Repository UserRepository for User {
            findById(id: UUID): User?
            findByEmail(email: String): User?
            save(user: User): Void
        }
    }
}
```

Corrected DDSL output:
```
BoundedContext UserManagement {
    domain {
        Aggregate User {
            @identity userId: UUID
            name: String @required @maxLength(100)
            email: String @required @email
            password: String @required @minLength(8)
            registeredAt: DateTime
            active: Boolean

            operations {
                when registering a new user with name, email, password {
                    require that email is not empty
                    require that password is not empty
                    then set active to true
                    then set registeredAt to now
                    emit event UserRegistered with userId, email
                }

                when deactivating user {
                    require that user must be currently active
                    then set active to false
                    emit event UserDeactivated with userId
                }

                when changing password with oldPassword, newPassword {
                    require that old password must match current
                    require that new password must be at least 8 chars
                    then update password
                    emit event PasswordChanged with userId
                }
            }
        }
    }
    events {
        DomainEvent UserRegistered {
            userId: UUID
            email: String
            registeredAt: DateTime
        }
        DomainEvent UserDeactivated {
            userId: UUID
            deactivatedAt: DateTime
        }
        DomainEvent PasswordChanged {
            userId: UUID
            changedAt: DateTime
        }
    }
    repositories {
        Repository UserRepository for User {
            findById(id: UUID): User?
            findByEmail(email: String): User?
            save(user: User): Void
        }
    }
}
```

Key transformations:
- `user id is UUID, this is the identity` → `@identity userId: UUID` — "this is the identity"→@identity
- `must be valid email` → `@email`
- `at least 8 characters` → `@minLength(8)` — natural constraint→formal
- `max 100 chars` → `@maxLength(100)`
- `default true` → dropped (DDSL has no default keyword; invariants or init logic handle defaults)
- Compressed `email and password cannot be empty, activate the user, record registration time, notify with UserRegistered event including userId and email` → split into: 2× `require that`, `then set active`, `then set registeredAt`, `emit event UserRegistered`
- `activate the user` → `then set active to true` — interpret intent
- `record registration time` → `then set registeredAt to now` — interpret intent
- `notify with UserRegistered event including userId and email` → `emit event UserRegistered with userId, email` — "notify with"→"emit", "including"→"with", "and"→comma
- `send UserDeactivated event with userId` → `emit event UserDeactivated with userId` — "send"→"emit event"
- `fire PasswordChanged event` → `emit event PasswordChanged with userId` — "fire"→"emit event"
- `user must be currently active` → `require that user must be currently active`
- All compressed comma-separated sentences → expanded into individual DDSL clauses
