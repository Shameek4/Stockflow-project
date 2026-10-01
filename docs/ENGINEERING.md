# Engineering and interview notes

## Request flow

Browser -> SecurityFilterChain (authentication, authorization, CSRF) -> ApiController
(validated request records) -> InventoryService (transaction/business rules) -> repositories
-> relational database. The UI and API share an origin. Static assets need no Node build.

## Why row locking?

Reading stock and saving a smaller number without a lock allows two transactions to sell
the same final item. `findLockedById` uses `PESSIMISTIC_WRITE`, so the second transaction
waits, then checks current stock. Product locks are acquired in ascending ID order to reduce
deadlocks in multi-product orders. Cancellation first locks the order, checks its status,
then locks its products in ascending order. Repeated cancellation returns the existing
cancelled order without adding stock again.

`@Transactional` puts every line, stock change, order and movement into one unit of work.
If a later line fails, earlier deductions are rolled back. Database uniqueness protects SKUs
when concurrent creation bypasses the earlier friendly duplicate check.

## Money and historical truth

Use BigDecimal with scale-limited inputs, not binary floating-point money arithmetic.
Orders copy the unit price and product name at creation, preserving historical values when
catalogue data changes. Browser currency formatting uses INR; this project does not implement
multiple currencies, taxes or payments. Confirmed sales total is order value, not proof of
payment. Inventory value is stock multiplied by selling price, not accounting cost.

## Permissions

Admin can mutate the product catalogue and inventory. Both admin and staff can view data,
create orders and cancel any order. This is a deliberately simple business permission model.
Passwords are hashed with BCrypt at startup. Accounts come from environment configuration;
there is no user table. Login uses session cookies and default Spring Security CSRF protection.

## Data model

- Product: unique SKU, name, category, price, stock, threshold and active flag.
- SalesOrder: customer, creator, timestamp, status and total.
- Order lines: embedded collection owned by the order, capturing product ID/name/price/quantity.
- StockMovement: product ID, delta, resulting stock, reason, actor and timestamp.

Products are archived instead of deleted. Audit records contain only stock movements.
Avoid changing database rows directly, as that bypasses application rules.

## Tests to discuss

The integration tests cover exact totals, whole-order rollback, price snapshots,
case-insensitive SKU conflicts, archived products, duplicate lines, stock bounds,
repeated and concurrent cancellation, simultaneous purchases of a final item, role restrictions,
CSRF, input validation, reporting and password login. The concurrency tests use separate
threads and service transactions rather than mocking repositories. They verify H2 behavior;
add real MySQL tests before claiming database-portable locking guarantees.

## Practice interview questions

1. Why is a stock availability check alone insufficient under concurrent requests?
2. What happens if the second item in an order is unavailable?
3. How do you prevent cancellation from restoring stock twice?
4. Why capture prices in order lines rather than look up today's product price?
5. Why is authorization enforced on the server even when the UI hides admin controls?
6. How would you add pagination, database-backed users, or product-ID-based reports?
7. How would you migrate the database safely in production?

Suggested resume bullet after you understand and customize the implementation:
"Built a Java/Spring Boot inventory and order application with role-based access,
transactional stock updates, cancellation workflows and automated concurrency tests."
