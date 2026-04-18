# Till POS System

A JavaFX-based point-of-sale desktop application with product management, payment processing, transaction logging, and end of day reporting.

---

## Requirements

- **Java JDK 21** or later — [Download here](https://www.oracle.com/java/technologies/downloads/)
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

In terminal 

```bash
mvn clean javafx:run
```


---

## Default Login

On first launch you will be presented with a login screen. Use admin123, can be changed later if you'd like. Admin users have access to stock management and the end of day report.

---

## Database

The application uses **SQLite** — no database setup is required. The database file is created automatically on first run at:
C:\Users<YourName>\AppData\Roaming\TillPOS\pos_system.db

A set of test products (Espresso, Cappuccino, Croissant, Cheesecake Slice) are inserted automatically on first run via `INSERT OR IGNORE` so they will not be duplicated on subsequent runs.

To remove the test data, comment out or delete the test data block in:
src/main/java/com/till/database/DatabaseConnection.java

---

## Features

- **Product browsing** by category with add-to-cart functionality
- **Cart management** — increase, decrease, or remove individual items
- **Cash and card payment** processing with change calculation
- **PDF receipts** generated automatically per transaction, saved to `receipts/`
- **Stock management** via the Admin panel — add, edit, and restock products
- **Transaction logging** to the SQLite database on every successful payment
- **End of Day report** — PDF summary of total sales, transaction count, and per-product breakdown, saved to `reports/`
- **Stock validation** — prevents payment if requested quantity exceeds available stock

---
##Only do this if your confident and you want to
## Packaging as a Desktop App

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
    │   │   └── SalesDAO.java
    │   ├── database/
    │   │   └── DatabaseConnection.java
    │   ├── model/                    # Data models
    │   │   ├── Product.java
    │   │   ├── OrderItem.java
    │   │   └── SalesRecord.java
    │   └── service/
    │       └── CartService.java
    └── resources/                    # FXML layout files
        ├── main-view.fxml
        ├── cart-pane.fxml
        ├── products-pane.fxml
        ├── admin-stock.fxml
        └── login.fxml
```

---

## Dependencies

All dependencies are managed via Maven and defined in `pom.xml`:

- **JavaFX 23** — UI framework
- **SQLite JDBC** — local database
- **iText 7** — PDF generation

---

## Notes

- Receipts are saved to `AppData\Roaming\TillPOS\receipts\` when running as a packaged app
- End of day reports are saved to `AppData\Roaming\TillPOS\reports\`
- The card payment includes a simulated 10% decline rate for testing purposes
