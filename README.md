# StockFlow

A working Java inventory and order management portfolio project for a fresher.

## What is included

- Dashboard: revenue, daily sales (UTC), inventory value, low-stock alerts, top products.
- Product creation, editing, search and archiving.
- Stock adjustment with mandatory reason and user attribution.
- Multi-item orders with exact decimal money calculations and price/name snapshots.
- Atomic stock deduction, pessimistic row locks, deterministic product lock order.
- Idempotent cancellation: stock is restored only once, including simultaneous cancellations.
- Session login, BCrypt passwords, admin/staff roles, CSRF protection, input validation.
- Responsive HTML/CSS/JavaScript interface served by the Java application.
- File-based H2 database by default; optional MySQL profile and Docker Compose.
- Automated integration tests and a GitHub Actions workflow.

## Quick start (download package)

The downloadable project ZIP includes `stockflow.jar`. If you cloned this source repository, use the Build from source section below to produce the JAR first.

Install Java 17 or newer. Open a terminal in this project directory.

**Windows PowerShell:**

```powershell
$env:STOCKFLOW_ADMIN_PASSWORD="ChooseYourAdminPassword123"
$env:STOCKFLOW_STAFF_PASSWORD="ChooseYourStaffPassword123"
java -jar stockflow.jar
```

**macOS / Linux:**

```bash
export STOCKFLOW_ADMIN_PASSWORD='ChooseYourAdminPassword123'
export STOCKFLOW_STAFF_PASSWORD='ChooseYourStaffPassword123'
java -jar stockflow.jar
```

Replace the example passwords with your own (at least 12 characters). Open
http://localhost:8080 and sign in as `admin` or `staff` with the corresponding password.
Use the admin account first to explore all features. Three sample products are added only
when the product table is empty and demo seeding is enabled.

The default server binds to your own computer (`127.0.0.1`). Data persists in `data/stockflow.mv.db`
relative to the directory from which you start Java. Stop with Ctrl+C. Keep that directory
consistent on each run. This is a local learning project, not a public production deployment.

## Build from source

Install Java 17+ and Maven 3.9+. From the directory containing `pom.xml`:

```bash
mvn clean verify
```

Set the two password environment variables above, then:

```bash
java -jar target/stockflow-1.0.0.jar
```

Alternatively run `mvn spring-boot:run`. The tests use an isolated in-memory database and
provide their own test-only passwords; they do not need your environment variables.

## Try this demo

1. Log in as admin and review the low-stock wireless mouse.
2. Open Inventory, adjust mouse stock by +10, reason "Supplier delivery".
3. Open Orders and create an order for 2 mice and 1 keyboard.
4. Check that quantities decrease and the dashboard shows the order revenue.
5. Cancel the order. Quantities return and revenue excludes the cancelled order.
6. Open Stock history to inspect every movement and its user.
7. Sign in as staff: you can view products and create/cancel orders; admin inventory mutations are forbidden.

## Optional MySQL

Install Docker Compose. Set `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD` to your own passwords
in the current terminal, then run:

```bash
docker compose up -d --wait
```

Set `DB_USER=stockflow` and, if needed, `DB_URL=jdbc:mysql://localhost:3306/stockflow`.
Keep the admin/staff password variables set, then:

```bash
java -jar stockflow.jar --spring.profiles.active=mysql
```

The MySQL configuration is supplied but the packaged test suite runs against H2. Test with
MySQL before relying on its locking behavior in a real deployment. Docker data is in a
named volume; do not delete that volume if you want to retain it.

## Project layout

```text
src/main/java/com/stockflow/       Java application, entities, services, API and security
src/main/resources/static/        Browser interface
src/main/resources/               Default and MySQL settings
src/test/java/com/stockflow/       Integration and concurrent transaction tests
docs/API.md                      Routes and permissions
docs/ENGINEERING.md              Design decisions and interview discussion
docs/VALIDATION.md               Verified checks and remaining limitations
compose.yaml                     Optional local MySQL
.github/workflows/ci.yml          CI
```

## Scope and next improvements

This version includes products, inventory, orders and reporting. Users are two configured
in-memory accounts; there is no registration, database-backed user management, payment
processing, email delivery or supplier purchasing. Product edits/archives are not separate
audit events; the audit table records quantity movements only. Report queries load the
small catalogue/order history in memory and have no pagination. Top-product rankings group
by the recorded product name, so renaming a product can split its historical ranking.

For a stronger second version: database-backed users, Flyway migrations, pagination,
product-ID-based reporting, MySQL Testcontainers tests, CSV exports and supplier purchase
orders. For public deployment: dependency review, HTTPS, secure cookies, login throttling,
secret management, backups and database migrations rather than `ddl-auto=update`.

## Portfolio use

Understand and customize the code before presenting it. Include screenshots and a demo video
in your GitHub repository. Describe only the features you have implemented and tested;
do not claim measured performance or production usage without evidence.
