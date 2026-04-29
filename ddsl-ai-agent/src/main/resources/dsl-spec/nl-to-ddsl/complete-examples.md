---
id: nl-to-ddsl-examples-001
category: NL_TO_DDSL
category: FEW_SHOT
subcategory: complete_examples
language: mixed
complexity: intermediate
version: 3.0
---

# Complete Examples: Natural Language to DDSL

## Overview

This document provides complete examples of transforming natural language descriptions into valid DDSL syntax. Each example shows the natural language input and the corresponding DDSL output.

---

## Example 1: Hotel Reservation System

### Natural Language Input

```
Bounded Context for Hotel Booking:

The Reservation aggregate has:
- reservation ID is UUID and serves as identity
- guest name is text and required
- check in date is date time and required
- check out date is date time and required
- room type is text and required
- total amount is money, cannot be negative
- status is text, default is PENDING
- created at is date time

The Room aggregate has:
- room ID is UUID and identity
- room number is text, required, must be unique
- floor is integer between 1 and 50
- room type is text and required
- base rate is decimal, minimum 0
- is available is boolean, default true

When placing reservation with guest and roomType:
- require that guest is not empty and roomType is not empty
- set status to PENDING
- calculate total from room rate
- emit ReservationPlaced with reservationId

When confirming reservation:
- require that status is PENDING and payment is received
- set status to CONFIRMED
- record confirmed at as now
- emit ReservationConfirmed with reservationId

When guest checks in:
- require that status is CONFIRMED
- set status to CHECKED_IN
- emit GuestCheckedIn with reservationId

When guest checks out:
- require that status is CHECKED_IN
- set status to CHECKED_OUT
- emit GuestCheckedOut with reservationId
```

### DDSL Output

```ddsl
BoundedContext HotelBooking {
    domain {
        Aggregate Reservation {
            reservationId: UUID @identity
            guestName: String @required
            checkInDate: DateTime @required
            checkOutDate: DateTime @required
            roomType: String @required
            totalAmount: Decimal @min(0)
            status: String @default("PENDING")
            createdAt: DateTime

            operations {
                when placing reservation with guest and roomType:
                    require that guest is not empty and roomType is not empty
                    set status to "PENDING"
                    calculate total from roomRate
                    emit ReservationPlaced with reservationId

                when confirming reservation:
                    require that status is "PENDING" and payment is received
                    set status to "CONFIRMED"
                    set confirmedAt to now
                    emit ReservationConfirmed with reservationId

                when guest checks in:
                    require that status is "CONFIRMED"
                    set status to "CHECKED_IN"
                    emit GuestCheckedIn with reservationId

                when guest checks out:
                    require that status is "CHECKED_IN"
                    set status to "CHECKED_OUT"
                    emit GuestCheckedOut with reservationId
            }
        }

        Aggregate Room {
            roomId: UUID @identity
            roomNumber: String @required @unique
            floor: Int @min(1) @max(50)
            roomType: String @required
            baseRate: Decimal @min(0)
            isAvailable: Boolean @default(true)
        }
    }

    events {
        DomainEvent ReservationPlaced {
            reservationId: UUID
            guestName: String
            roomType: String
            totalAmount: Decimal
        }

        DomainEvent ReservationConfirmed {
            reservationId: UUID
            confirmedAt: DateTime
        }

        DomainEvent GuestCheckedIn {
            reservationId: UUID
            checkedInAt: DateTime
        }

        DomainEvent GuestCheckedOut {
            reservationId: UUID
            checkedOutAt: DateTime
        }
    }
}
```

---

## Example 2: E-Commerce Order System

### Natural Language Input

```
Bounded Context for Order Management:

The Order aggregate has:
- order ID is UUID and identity
- customer ID is UUID and required
- order items is list of OrderItem, at least one required
- total amount is decimal, minimum 0
- status is text, default is PENDING
- shipping address is text and required
- created at is date time

The OrderItem entity has:
- item ID is UUID and identity
- product name is text and required
- quantity is integer, at least 1
- unit price is decimal, minimum 0

The Customer value object has:
- customer ID is UUID and identity
- name is text, required, max 200 characters
- email is text, required, must be valid email
- phone is text, optional

When placing order with customerId and items:
- require that customerId is not empty and items is not empty
- calculate total as sum of items quantity times unit price
- set status to PENDING
- emit OrderPlaced with orderId and customerId

When confirming order:
- require that status is PENDING
- set status to CONFIRMED
- emit OrderConfirmed with orderId

When shipping order:
- require that status is CONFIRMED
- set status to SHIPPED
- emit OrderShipped with orderId

When delivering order:
- require that status is SHIPPED
- set status to DELIVERED
- record delivered at as now
- emit OrderDelivered with orderId
```

### DDSL Output

```ddsl
BoundedContext OrderManagement {
    domain {
        Aggregate Order {
            orderId: UUID @identity
            customerId: UUID @required
            orderItems: List<OrderItem> @min(1)
            totalAmount: Decimal @min(0)
            status: String @default("PENDING")
            shippingAddress: String @required
            createdAt: DateTime

            Entity OrderItem {
                itemId: UUID @identity
                productName: String @required
                quantity: Int @min(1)
                unitPrice: Decimal @min(0)
            }

            operations {
                when placing order with customerId and items:
                    require that customerId is not empty and items is not empty
                    calculate total as sum of items quantity times unitPrice
                    set status to "PENDING"
                    emit OrderPlaced with orderId and customerId

                when confirming order:
                    require that status is "PENDING"
                    set status to "CONFIRMED"
                    emit OrderConfirmed with orderId

                when shipping order:
                    require that status is "CONFIRMED"
                    set status to "SHIPPED"
                    emit OrderShipped with orderId

                when delivering order:
                    require that status is "SHIPPED"
                    set status to "DELIVERED"
                    set deliveredAt to now
                    emit OrderDelivered with orderId
            }
        }

        ValueObject Customer {
            customerId: UUID @identity
            name: String @required @maxLength(200)
            email: String @required @email
            phone: String?
        }
    }

    events {
        DomainEvent OrderPlaced {
            orderId: UUID
            customerId: UUID
            totalAmount: Decimal
        }

        DomainEvent OrderConfirmed {
            orderId: UUID
            confirmedAt: DateTime
        }

        DomainEvent OrderShipped {
            orderId: UUID
            shippedAt: DateTime
        }

        DomainEvent OrderDelivered {
            orderId: UUID
            deliveredAt: DateTime
        }
    }
}
```

---

## Example 3: Library Management System

### Natural Language Input

```
Bounded Context for Library Management:

The Book aggregate has:
- book ID is UUID and identity
- ISBN is text, required, must be unique
- title is text, required, max 500 characters
- author is text and required
- publication year is integer
- total copies is integer, minimum 1
- available copies is integer, minimum 0
- is available is boolean, default true

The Member aggregate has:
- member ID is UUID and identity
- name is text, required, max 200 characters
- email is text, required, unique, valid email format
- phone is text, optional
- membership date is date time
- is active is boolean, default true

When borrowing book with memberId and bookId:
- require that memberId is not empty and bookId is not empty
- require that book is available and member is active
- decrease available copies by 1
- create loan record with memberId and bookId
- emit BookBorrowed with bookId and memberId

When returning book with loanId:
- require that loanId is not empty
- increase available copies by 1
- update loan status to RETURNED
- emit BookReturned with bookId
```

### DDSL Output

```ddsl
BoundedContext LibraryManagement {
    domain {
        Aggregate Book {
            bookId: UUID @identity
            isbn: String @required @unique
            title: String @required @maxLength(500)
            author: String @required
            publicationYear: Int
            totalCopies: Int @min(1)
            availableCopies: Int @min(0)
            isAvailable: Boolean @default(true)

            operations {
                when borrowing book with memberId and bookId:
                    require that memberId is not empty and bookId is not empty
                    require that isAvailable is true
                    set availableCopies to availableCopies minus 1
                    emit BookBorrowed with bookId and memberId

                when returning book with loanId:
                    require that loanId is not empty
                    set availableCopies to availableCopies plus 1
                    emit BookReturned with bookId
            }
        }

        Aggregate Member {
            memberId: UUID @identity
            name: String @required @maxLength(200)
            email: String @required @unique @email
            phone: String?
            membershipDate: DateTime
            isActive: Boolean @default(true)
        }
    }

    events {
        DomainEvent BookBorrowed {
            bookId: UUID
            memberId: UUID
            borrowedAt: DateTime
        }

        DomainEvent BookReturned {
            bookId: UUID
            memberId: UUID
            returnedAt: DateTime
        }
    }
}
```

---

## Example 4: Vietnamese Input - Food Delivery

### Natural Language Input (Vietnamese)

```
Bounded Context cho hệ thống giao đồ ăn:

Aggregate Đơn hàng có:
- mã đơn hàng là UUID và là định danh
- mã khách hàng là UUID và bắt buộc
- danh sách món ăn là list của OrderItem, ít nhất 1 món
- tổng tiền là số thực, không âm
- trạng thái là chuỗi, mặc định là PENDING
- địa chỉ giao hàng là chuỗi và bắt buộc
- ngày tạo là thờ gian

Khi đặt hàng với customerId và items:
- yêu cầu customerId không rỗng và items không rỗng
- tính tổng tiền từ các món
- đặt trạng thái là PENDING
- phát sự kiện OrderPlaced với orderId

Khi xác nhận đơn hàng:
- yêu cầu trạng thái là PENDING
- đặt trạng thái là CONFIRMED
- phát sự kiện OrderConfirmed với orderId

Khi giao hàng:
- yêu cầu trạng thái là CONFIRMED
- đặt trạng thái là DELIVERING
- phát sự kiện OrderDelivering với orderId
```

### DDSL Output

```ddsl
BoundedContext FoodDelivery {
    domain {
        Aggregate Order {
            orderId: UUID @identity
            customerId: UUID @required
            items: List<OrderItem> @min(1)
            totalAmount: Decimal @min(0)
            status: String @default("PENDING")
            deliveryAddress: String @required
            createdAt: DateTime

            operations {
                when placing order with customerId and items:
                    require that customerId is not empty and items is not empty
                    calculate totalAmount from items
                    set status to "PENDING"
                    emit OrderPlaced with orderId

                when confirming order:
                    require that status is "PENDING"
                    set status to "CONFIRMED"
                    emit OrderConfirmed with orderId

                when delivering order:
                    require that status is "CONFIRMED"
                    set status to "DELIVERING"
                    emit OrderDelivering with orderId
            }
        }
    }

    events {
        DomainEvent OrderPlaced {
            orderId: UUID
            customerId: UUID
            totalAmount: Decimal
        }

        DomainEvent OrderConfirmed {
            orderId: UUID
            confirmedAt: DateTime
        }

        DomainEvent OrderDelivering {
            orderId: UUID
            deliveringAt: DateTime
        }
    }
}
```

---

## Example 5: Complex Domain with Specifications

### Natural Language Input

```
Bounded Context for Product Catalog:

The Product aggregate has:
- product ID is UUID and identity
- SKU is text, required, unique, max 50 characters
- name is text, required, max 200 characters
- description is text, optional, max 2000 characters
- base price is decimal, minimum 0
- category is text and required
- is active is boolean, default true
- created at is date time
- updated at is date time

When creating product with sku and name and price:
- require that sku is not empty and name is not empty and price is not empty
- require that price is greater than 0
- set is active to true
- set created at as now
- emit ProductCreated with productId

When updating price with newPrice:
- require that newPrice is greater than 0
- set base price to newPrice
- set updated at as now
- emit PriceUpdated with productId and newPrice

When deactivating product:
- require that is active is true
- set is active to false
- set updated at as now
- emit ProductDeactivated with productId

Specifications:
- ActiveProducts: matches products where is active is true
- HighValueProducts: matches products where base price is greater than 1000
- InCategory with category: matches products where category equals given category
```

### DDSL Output

```ddsl
BoundedContext ProductCatalog {
    domain {
        Aggregate Product {
            productId: UUID @identity
            sku: String @required @unique @maxLength(50)
            name: String @required @maxLength(200)
            description: String? @maxLength(2000)
            basePrice: Decimal @min(0)
            category: String @required
            isActive: Boolean @default(true)
            createdAt: DateTime
            updatedAt: DateTime

            operations {
                when creating product with sku and name and price:
                    require that sku is not empty and name is not empty and price is not empty
                    require that price is greater than 0
                    set isActive to true
                    set createdAt to now
                    emit ProductCreated with productId

                when updating price with newPrice:
                    require that newPrice is greater than 0
                    set basePrice to newPrice
                    set updatedAt to now
                    emit PriceUpdated with productId and newPrice

                when deactivating product:
                    require that isActive is true
                    set isActive to false
                    set updatedAt to now
                    emit ProductDeactivated with productId
            }
        }
    }

    events {
        DomainEvent ProductCreated {
            productId: UUID
            sku: String
            name: String
            basePrice: Decimal
        }

        DomainEvent PriceUpdated {
            productId: UUID
            oldPrice: Decimal
            newPrice: Decimal
        }

        DomainEvent ProductDeactivated {
            productId: UUID
            deactivatedAt: DateTime
        }
    }

    specifications {
        Specification ActiveProducts {
            matches products where:
                - isActive is true
        }

        Specification HighValueProducts {
            matches products where:
                - basePrice is greater than 1000
        }

        Specification InCategory given category {
            matches products where:
                - category is category
        }
    }
}
```

---

## Key Transformation Patterns from Examples

### 1. Field Declaration Pattern

**Natural:** `field name is type and constraint`
**DDSL:** `fieldName: Type @constraint`

Examples:
- `order ID is UUID and identity` → `orderId: UUID @identity`
- `name is text, required, max 200 chars` → `name: String @required @maxLength(200)`
- `price is decimal, minimum 0` → `price: Decimal @min(0)`

### 2. Collection Pattern

**Natural:** `field is list of Type`
**DDSL:** `field: List<Type>`

Examples:
- `items is list of OrderItem` → `items: List<OrderItem>`
- `tags is set of String` → `tags: Set<String>`

### 3. Behavior Pattern

**Natural:**
```
When action with params:
- require condition and condition
- set field to value
- emit Event with props
```

**DDSL:**
```ddsl
when action with params:
    require that condition and condition
    set field to value
    emit Event with props
```

### 4. Event Declaration Pattern

**Natural:** `EventName has field1 is type1, field2 is type2`
**DDSL:**
```ddsl
DomainEvent EventName {
    field1: Type1
    field2: Type2
}
```

### 5. Vietnamese to English Mapping

| Vietnamese | English DDSL |
|------------|--------------|
| mã | id/code |
| tên | name |
| danh sách | list |
| số thực | Decimal |
| chuỗi | String |
| bắt buộc | @required |
| ngày tạo | createdAt |
| trạng thái | status |
| tổng tiền | totalAmount |
