package com.till.database;

import com.till.config.AppConfig;
import com.till.security.PasswordHasher;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static final String DB_PATH;
    private static final String URL;
    private static final AtomicBoolean INITIALISED = new AtomicBoolean(false);

    static {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File dir = new File(appData + "/TillPOS");
            dir.mkdirs();
            DB_PATH = dir.getAbsolutePath() + "/pos_system.db";
        } else {
            DB_PATH = "pos_system.db";
        }
        URL = "jdbc:sqlite:" + DB_PATH;
    }

    public static String getDbPath() {
        return DB_PATH;
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        }

        if (INITIALISED.compareAndSet(false, true)) {
            initTables(connection);
        }

        return connection;
    }

    private static void initTables(Connection connection) {
        String products = """
            CREATE TABLE IF NOT EXISTS products (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                stock INTEGER DEFAULT 0,
                category TEXT,
                barcode TEXT
            )
        """;

        String orders = """
            CREATE TABLE IF NOT EXISTS orders (
                order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                total REAL NOT NULL,
                cash_given REAL,
                change REAL,
                payment_method TEXT DEFAULT 'CASH',
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String orderItems = """
            CREATE TABLE IF NOT EXISTS order_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER,
                product_id TEXT,
                product_name TEXT,
                quantity INTEGER,
                price REAL,
                subtotal REAL,
                FOREIGN KEY(order_id) REFERENCES orders(order_id),
                FOREIGN KEY(product_id) REFERENCES products(id)
            )
        """;

        String auditEvents = """
            CREATE TABLE IF NOT EXISTS audit_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_type TEXT NOT NULL,
                details TEXT,
                actor TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String syncQueue = """
            CREATE TABLE IF NOT EXISTS transaction_sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                payload_json TEXT NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'PENDING',
                last_error TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                next_attempt_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME
            )
        """;

        String users = """
            CREATE TABLE IF NOT EXISTS users (
                username TEXT PRIMARY KEY,
                role TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(products);
            stmt.execute(orders);
            stmt.execute(orderItems);
            stmt.execute(auditEvents);
            stmt.execute(syncQueue);
            stmt.execute(users);
            LOGGER.info("Database tables ready");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise database tables", e);
        }

        runMigrations(connection);
        seedDefaultUsers(connection);

        if (AppConfig.seedSampleData()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                    INSERT OR IGNORE INTO products (id, name, price, category, stock)
                    VALUES
                        ('P001', 'Espresso',         3.20, 'Beverages',     45),
                        ('P002', 'Cappuccino',        3.80, 'Beverages',     28),
                        ('P003', 'Latte',             3.50, 'Beverages',     30),
                        ('P004', 'Cola',              1.80, 'Beverages',     50),
                        ('P005', 'Water',             1.00, 'Beverages',     60),
                        ('P006', 'Juice',             2.00, 'Beverages',     40),
                        ('P007', 'Croissant',         2.50, 'Bakery',        15),
                        ('P008', 'Doughnut',          1.80, 'Bakery',        20),
                        ('P009', 'Baguette',          2.20, 'Bakery',        18),
                        ('P010', 'Cheesecake Slice',  4.50, 'Desserts',      12),
                        ('P011', 'Ice Cream',         3.00, 'Desserts',      25),
                        ('P012', 'Brownie',           2.80, 'Desserts',      18),
                        ('P013', 'Cheese',            2.00, 'Miscellaneous', 30),
                        ('P014', 'Knife',            30.00, 'Miscellaneous',  5),
                        ('P015', 'Chewing Tobacco',  10.00, 'Tobacco',       20),
                        ('P016', 'Cigarettes',        9.50, 'Tobacco',       35),
                        ('P017', 'Vodka',            30.00, 'Alcohol',       15),
                        ('P018', 'Whisky',           20.00, 'Alcohol',       12),
                        ('P019', 'Beer',              4.50, 'Alcohol',       40),
                        ('P020', 'Wine',              6.00, 'Alcohol',       25),
                        ('P021', 'Crisps',            1.20, 'Snacks',        50),
                        ('P022', 'Chocolate Bar',     1.50, 'Snacks',        45),
                        ('P023', 'Nuts',              2.50, 'Snacks',        30)
                """);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Sample data insert failed", e);
            }
        }
    }

    private static void seedDefaultUsers(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                String managerSalt = PasswordHasher.generateSalt();
                String managerHash = PasswordHasher.hashPassword("admin123", managerSalt);
                stmt.executeUpdate(String.format(
                        "INSERT INTO users (username, role, password_hash, password_salt, active) " +
                                "VALUES ('manager', 'MANAGER', '%s', '%s', 1)",
                        managerHash, managerSalt));

                String cashierSalt = PasswordHasher.generateSalt();
                String cashierHash = PasswordHasher.hashPassword("cashier123", cashierSalt);
                stmt.executeUpdate(String.format(
                        "INSERT INTO users (username, role, password_hash, password_salt, active) " +
                                "VALUES ('cashier', 'CASHIER', '%s', '%s', 1)",
                        cashierHash, cashierSalt));

                LOGGER.info("Default users seeded: manager / cashier");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to seed default users", e);
        }
    }

    private static void runMigrations(Connection connection) {
        String[] migrations = {
                "ALTER TABLE orders ADD COLUMN payment_method TEXT DEFAULT 'CASH'",
                "ALTER TABLE order_items ADD COLUMN product_name TEXT",
                "ALTER TABLE order_items ADD COLUMN subtotal REAL",
                "ALTER TABLE transaction_sync_queue ADD COLUMN next_attempt_at DATETIME DEFAULT CURRENT_TIMESTAMP"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String migration : migrations) {
                try {
                    stmt.execute(migration);
                    LOGGER.fine("Migration applied: " + migration);
                } catch (SQLException e) {
                    LOGGER.fine("Migration skipped: " + migration);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Migration run failed", e);
        }
    }

    public static void close() {
        // Kept for backwards compatibility with existing call sites.
        // Connections are now created per-request and closed by try-with-resources.
    }
}