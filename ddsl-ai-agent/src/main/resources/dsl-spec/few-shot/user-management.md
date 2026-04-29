---
id: ddsl-fewshot-004
category: FEW_SHOT
subcategory: informal_mixed
language: en
complexity: intermediate
version: 3.0
---

This example shows how to handle a mix of very informal descriptions — compressed behaviors, loose field definitions, casual event and repository syntax — using the natural DDSL format.

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
```ddsl
BoundedContext UserManagement {
    domain {
        Aggregate User {
            userId: UUID @identity
            name: String @required @maxLength(100)
            email: String @required @email
            password: String @required @minLength(8)
            registeredAt: DateTime
            active: Boolean @default(true)

            operations {
                when registering user with name and email and password:
                    require that email is not empty and password is not empty
                    set active to true
                    set registeredAt to now
                    emit UserRegistered with userId and email

                when deactivating user:
                    require that active is true
                    set active to false
                    emit UserDeactivated with userId

                when changing password with oldPassword and newPassword:
                    require that oldPassword matches current and newPassword length is at least 8
                    set password to newPassword
                    emit PasswordChanged with userId
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
- `user id is UUID, this is the identity` → `userId: UUID @identity` — "this is the identity"→@identity
- `must be valid email` → `@email`
- `at least 8 characters` → `@minLength(8)` — natural constraint→formal
- `max 100 chars` → `@maxLength(100)`
- `default true` → `@default(true)`
- Compressed `email and password cannot be empty, activate the user, record registration time, notify with UserRegistered event including userId and email` → natural format: `require that email is not empty and password is not empty` (combined), then actions flow as sentences
- `activate the user` → `set active to true` — interpret intent
- `record registration time` → `set registeredAt to now` — interpret intent
- `notify with UserRegistered event including userId and email` → `emit UserRegistered with userId and email` — "notify with"→"emit", "including"→"with"
- `send UserDeactivated event with userId` → `emit UserDeactivated with userId` — "send"→"emit"
- `fire PasswordChanged event` → `emit PasswordChanged with userId` — "fire"→"emit"
- `user must be currently active` → `require that active is true`
- `old password must match current, new password must be at least 8 chars` → `require that oldPassword matches current and newPassword length is at least 8` — combined with "and"
- **Natural format**: Combined requires with "and", no bullet points, actions as readable sentences, removed "then" keyword for cleaner flow
