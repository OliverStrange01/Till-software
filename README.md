# Till POS System

A JavaFX-based point-of-sale desktop application with product and stock management, coupon discounts, PDF receipt generation, transaction logging, API sync support, assistance-call checks, and end-of-day reporting.

---

## Requirements

- **Java JDK 17+** (project compiles with source/target 17) — [Download here](https://www.oracle.com/java/technologies/downloads/)
- **Maven** — [Download here](https://maven.apache.org/download.cgi)
- An IDE such as **IntelliJ IDEA** (recommended) or Eclipse

---

## Getting Started

### 1. Clone or download the repository

Download and extract the ZIP, or clone via Git:

```bash
git clone --branch update https://github.com/OliverStrange01/Till-software.git
cd till-system
```

### 2. Open in IntelliJ IDEA

- Open IntelliJ and select **File > Open**
- Navigate to the project folder and click **OK**
- IntelliJ should automatically detect the `pom.xml` and import the Maven project
- Wait for dependencies to finish downloading

### 3. Build the project

In IntelliJ, open the **Maven** panel (right side) and run:
Lifecycle > clean
Lifecycle > package

Or via terminal:

```bash
mvn clean package
```

### 4. Run the application

In terminal:

```bash
mvn clean javafx:run
```

If `clean` fails on Windows due file locks in `target/`, run without `clean`:

```bash
mvn javafx:run
```

If `mvn` is not yet on your `PATH` (common on Windows), use the full Maven path and set `JAVA_HOME` for the current shell:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-26"
& "C:\Program Files\maven\apache-maven-3.9.15\bin\mvn.cmd" clean javafx:run
```

### 5. Run tests

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-26"
& "C:\Program Files\maven\apache-maven-3.9.15\bin\mvn.cmd" test
```

If `mvn clean ...` fails with "Failed to delete target", close running app windows/terminals and retry, or run without `clean`.

### 6. Teammate quick start (recommended)

For a fast first run on Windows:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-26"
& "C:\Program Files\maven\apache-maven-3.9.15\bin\mvn.cmd" test
& "C:\Program Files\maven\apache-maven-3.9.15\bin\mvn.cmd" javafx:run
```

Use one of these accounts:
- Manager: `manager` / `admin123`
- Cashier: `cashier` / `cashier123`

### 6. Optional environment variables

- `TILL_TX_API_URL` - transaction API endpoint
- `TILL_TX_API_KEY` - transaction API bearer token
- `TILL_TX_SYNC_BATCH_SIZE` - startup retry batch size for queued API sync records
- `TILL_TX_SYNC_MAX_ATTEMPTS` - max retry attempts before dead-letter state
- `TILL_TX_SYNC_BASE_DELAY_SECONDS` - base backoff delay for retries
- `TILL_SEED_SAMPLE_DATA` - set `true` to seed sample products


---

## Default Login

On first launch you will be presented with a single login form that requires:
- **Username**
- **Password**

Default credentials:
- **Manager**: `manager` / `admin123`
- **Cashier**: `cashier` / `cashier123`

Role access:
- **Manager** can access stock management, end-of-day reporting, and audit/queue visibility.
- **Cashier** can use normal checkout features but not manager-only admin tools.

Important behaviour change:
- The older "Login as Cashier (No Password)" button was removed.
- Both roles now log in through the same username/password screen.

If login fails:
1. Check username spelling exactly (`manager` or `cashier`).
2. Check password exactly (`admin123` or `cashier123`).
3. Restart the app after pull/build changes.

---

## Database

The application uses **SQLite** — no database setup is required. The database file is created automatically on first run at:

`C:\Users\<YourName>\AppData\Roaming\TillPOS\pos_system.db`

A set of sample products can be seeded on first run using an environment flag:

```powershell
$env:TILL_SEED_SAMPLE_DATA="true"
```

If this variable is not set to `true`, sample products are not inserted.

Seeding logic lives in:

`src/main/java/com/till/database/DatabaseConnection.java`

---

## Features

- **Product browsing by category** with add-to-cart functionality
- **Cart management** with quantity increase/decrease and item removal
- **Discounts and coupons** with validation (expiry, minimum spend, and stacking rules)
- **Cash and card payments** with change calculation
- **Assistance calls** for manager override, security checks, and price checks
- **PDF receipt generation** for each successful transaction
- **Transaction API sync support** (via `TILL_TX_API_URL` and `TILL_TX_API_KEY`)
- **Offline transaction retry queue** for failed API sync attempts
- **Exponential backoff retry policy** with dead-letter state after max attempts
- **Audit trail logging** for coupon, assistance, payment, and sync events
- **Audit/queue visibility UI** for operational monitoring
- **SQLite transaction logging** on successful payment
- **Admin stock management** for restocking and product creation
- **End-of-day PDF reporting** with transaction totals and product breakdown
- **Unit tests** for discount logic, assistance logic, and transaction API client behaviour

---
## Authentication and Security Model

- Authentication is database-backed (`users` table), not hardcoded plain-text checks.
- Passwords are stored as **salted PBKDF2-HMAC-SHA256 hashes**.
- Default users are auto-created if missing:
  - `manager`
  - `cashier`
- Role checks are used to control manager-only screens and actions.

---
## Transactions API and Retry Policy

- Successful payments are logged locally first, then synced to API.
- If API sync fails, payloads are queued in `transaction_sync_queue`.
- Retry policy includes:
  - scheduled retry using `next_attempt_at`
  - exponential backoff delay
  - max-attempt cut-off to `DEAD` status
- Queue retries are processed on startup.

Relevant configuration:
- `TILL_TX_API_URL`
- `TILL_TX_API_KEY`
- `TILL_TX_SYNC_BATCH_SIZE`
- `TILL_TX_SYNC_MAX_ATTEMPTS`
- `TILL_TX_SYNC_BASE_DELAY_SECONDS`

---
## Audit and Queue Visibility

- Audit events are recorded in `audit_events` (coupon, assistance, payment, sync).
- Managers can open the **Audit / Queue** monitor from the main screen.
- The monitor shows:
  - recent audit events
  - queue status, attempts, next retry, and last error

---
## What Changed In This Iteration

The following work was added in this implementation pass:

### 1) Core feature delivery
- Added **coupon/discount support** with rule validation:
  - percentage and fixed discounts
  - minimum spend checks
  - expiry checks
  - non-stacking behaviour by default
- Added **PDF receipt generation** service and checkout integration.
- Added **transaction API sync** support for completed payments.
- Added **assistance-call checks** (manager override/security/price checks) before payment completion.

### 2) Reliability and architecture improvements
- Refactored checkout flow so `MainController` delegates to dedicated services.
- Added central config via `AppConfig` for environment-based settings.
- Added **offline retry queue** (`transaction_sync_queue`) for failed API sync attempts.
- Added **strong retry policy** (scheduled retries with exponential backoff and dead-letter state).
- Added **audit logging** (`audit_events`) for coupon, assistance, payment, and sync actions.
- Added **audit/queue monitor UI** for admins.
- Improved logging and general code hygiene (removed debug prints/unused imports, tightened null handling, safer DAO patterns).

### 3) Security/auth and environment updates
- Login now uses a **database-backed role model** with salted PBKDF2 password hashing.
- Default users are seeded safely if missing (`manager`, `cashier`) and credentials are verified by role.
- Maven/JDK run instructions updated for Windows environments where `mvn` is not on `PATH`.

### 4) Testing and verification
- Added/expanded unit tests:
  - `AuthDAOTest`
  - `RetryPolicyIntegrationTest`
  - `DiscountServiceTest`
  - `AssistanceServiceTest`
  - `TransactionApiClientTest`
  - `TransactionSyncDAOTest`
  - `AppConfigTest`
- Current automated status: **12 tests passing** (`mvn test`).

Full session contribution log:
- `SESSION_CONTRIBUTIONS.txt`

### 5) Clarification for reviewers/users
- This project now uses role-based username/password login.
- There are still two user types (manager and cashier), but one shared login form.
- The project structure section is a concise overview, not an exhaustive file listing.

---
## Project Progress Status

This project is now in a **strong MVP / pre-production** state.

### Overall progress (estimated)
- **Core POS workflow:** ~95% complete
- **Requested feature set (discounts, receipts, API transactions, assistance):** ~95% complete
- **Production hardening (security, resilience, auditability):** ~75% complete
- **Overall project line:** ~85% complete

### What is complete
- End-to-end checkout flow with stock validation.
- Discounts/coupons integrated into totals.
- PDF receipts and end-of-day PDF reports.
- Local transaction logging plus outbound API sync.
- Offline queue and startup retry for failed sync attempts.
- Audit trail table and logging for key operational events.
- Unit test coverage for key business services.

### What remains before "fully complete"
- Add password reset/change workflow and account lockout policy.
- Add deeper end-to-end integration tests for full checkout-to-sync lifecycle.
- Expand admin monitoring UI with filtering/search/export.
- Final deployment packaging polish (`jpackage` profile, signing/distribution process).

### Important clarification
- The structure tree below is a concise overview, not an exhaustive list of all files.
- Additional key packages now include:
  - `src/main/java/com/till/config/`
  - `src/main/java/com/till/security/`
  - `src/main/resources/audit-queue.fxml`
  - `src/test/java/com/till/dao/`

---
## Packaging (Optional)

To build a standalone `.exe` that does not require Java to be installed:

### 1. Build the fat JAR

```bash
mvn clean package
```

### 2. Run jpackage

```powershell
& "C:\Program Files\Java\jdk-21\bin\jpackage.exe" `
  --input target `
  --name "Till POS" `
  --main-jar till-system-1.0-SNAPSHOT.jar `
  --main-class com.till.Launcher `
  --type app-image `
  --win-console
```

This produces a `Till POS` folder containing `Till POS.exe`. The entire folder must be kept together — do not move the `.exe` out of it. To run from the desktop, right click `Till POS.exe` and create a shortcut, then place the shortcut on the desktop.

> For a proper `.msi` or `.exe` installer, install [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases) and replace `--type app-image` with `--type exe`.

---

## Project Structure
```
src/
├── test/java/com/till/                 # Tests (service + DAO)
│   ├── service/
│   │   ├── DiscountServiceTest.java
│   │   ├── AssistanceServiceTest.java
│   │   ├── TransactionApiClientTest.java
│   │   └── AppConfigTest.java
│   └── dao/
│       ├── TransactionSyncDAOTest.java
│       ├── AuthDAOTest.java
│       └── RetryPolicyIntegrationTest.java
└── main/
    ├── java/com/till/
    │   ├── App.java                  # JavaFX entry point
    │   ├── Launcher.java             # Fat JAR launcher wrapper
    │   ├── controller/               # FXML controllers
    │   │   ├── MainController.java
    │   │   ├── CartController.java
    │   │   ├── ProductCategoryController.java
    │   │   ├── AdminStockController.java
    │   │   └── LoginController.java
    │   ├── dao/                      # Database access
    │   │   ├── ProductDAO.java
    │   │   ├── SalesDAO.java
    │   │   ├── AuditDAO.java
    │   │   ├── TransactionSyncDAO.java
    │   │   └── AuthDAO.java
    │   ├── config/
    │   │   └── AppConfig.java
    │   ├── database/
    │   │   └── DatabaseConnection.java
    │   ├── security/
    │   │   └── PasswordHasher.java
    │   ├── model/                    # Data models
    │   │   ├── Product.java
    │   │   ├── OrderItem.java
    │   │   ├── SalesRecord.java
    │   │   ├── Coupon.java
    │   │   ├── DiscountRule.java
    │   │   ├── DiscountType.java
    │   │   ├── DiscountApplicationResult.java
    │   │   ├── AssistanceCall.java
    │   │   ├── AssistanceType.java
    │   │   ├── ReceiptData.java
    │   │   └── TransactionPayload.java
    │   └── service/
    │       ├── CartService.java
    │       ├── DiscountService.java
    │       ├── AssistanceService.java
    │       ├── ReceiptService.java
    │       └── TransactionApiClient.java
    └── resources/                    # FXML layout files
        ├── main.fxml
        ├── cart-pane.fxml
        ├── products-pane.fxml
        ├── admin-stock.fxml
        ├── audit-queue.fxml
        └── login.fxml
```

---

## Dependencies

All dependencies are managed via Maven and defined in `pom.xml`:

- **JavaFX 23** — UI framework
- **SQLite JDBC** — local database
- **iText 7** — PDF generation
- **JUnit 5** — unit testing

---

## Notes

- Receipts are saved to `receipts/` when running from source; packaged app output may differ by launcher configuration.
- End-of-day reports are saved to `reports/` when running from source.
- The card payment includes a simulated 10% decline rate for testing purposes
- If `mvn` is not recognised, either restart the terminal after updating `PATH` or use the full `mvn.cmd` path shown above.
- If login appears to fail after updates, verify username/password pair and restart the app to load the latest classes/resources.
