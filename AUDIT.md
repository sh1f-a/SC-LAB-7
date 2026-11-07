# Code Smells and SOLID Audit: OrderBillingEngine

## Part A: Code Smells

| # | Smell | Lines | Why it is bad | Planned fix |
|---|-------|-------|---------------|-------------|
| 1 | God Class | 15-150 (whole class) | One class does validation, discounts, tax, payment, file logging and notifications. It has many reasons to change. | Split into DiscountCalculator, TaxCalculator, PaymentStrategy classes, OrderRepository, NotificationService, and a thin coordinating service. |
| 2 | Long Method | 20-150 (processOrder) | One method of about 130 lines does everything. It is hard to read, test and change. | Extract each responsibility into its own small class or method. |
| 3 | Magic Numbers | 70-86 (discounts), 91-98 (tax), 123 (COD limit), 5000, 10000, 10 | Values like 0.15, 0.10, 0.08, 0.17, 25000 have no names, so their meaning is unclear and changes are risky. | Replace with named constants inside the calculator classes. |
| 4 | Primitive Obsession | 29, 62, 90, 100, 139 | Customer type, country, payment method and notify channel are plain Strings. A typo like "VIPP" fails silently. The order is a Map<String,Object> with casts. | Use enums and a proper Order class. |
| 5 | Direct File I/O | 131-136 | FileWriter is used inside business logic, so the logic cannot be tested without touching the disk. | Move it behind an OrderRepository interface. |
| 6 | Deeply nested if-else chains | 66-86, 93-98, 104-126, 139-147 | Hard to read, and every new rule means editing the chain. | Replace with Strategy objects and lookup maps. |
| 7 | Console printing for errors and notifications | 22-24, 28-30, 117, 141, 145 | System.out.println is used as error handling and as the notification system. It cannot be tested or redirected. | Use a NotificationService interface and return clear results. |
| 8 | Unsafe casts | 27, 33, 53-54, 100 | Casts from Object throw ClassCastException if the data has the wrong type. | Use a typed Order and OrderItem class. |

## Part B: SOLID Violations

| # | Principle | Lines | What is broken | Planned fix |
|---|-----------|-------|----------------|-------------|
| 1 | Single Responsibility (SRP) | 15-150 | The class changes for six different reasons: validation, discount rules, tax rules, payment rules, logging, notifications. | One class per responsibility. |
| 2 | Open/Closed (OCP) | 104-126 and 139-147 | Adding JazzCash or WhatsApp means editing the existing if-else chains. | PaymentStrategy and NotificationService interfaces. New methods are new classes. |
| 3 | Dependency Inversion (DIP) | 131 and 141-146 | The high-level logic depends directly on concrete FileWriter and System.out. | Depend on OrderRepository and NotificationService interfaces, injected through the constructor. |