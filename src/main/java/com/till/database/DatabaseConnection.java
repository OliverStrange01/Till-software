package com.till.database;

import com.till.config.AppConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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
                barcode TEXT,
                low_stock_threshold INTEGER DEFAULT 10,
                is_weighted INTEGER DEFAULT 0,
                unit TEXT DEFAULT 'each',
                allergens TEXT DEFAULT ''
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
                quantity REAL,
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

        // Seed data is useful in development, but should not run in production by default.
        if (AppConfig.seedSampleData()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                    INSERT OR IGNORE INTO products (id, name, price, category, stock)
                    VALUES
                        ('P001', 'Espresso', 3.20, 'Beverages', 45),
                        ('P002', 'Cappuccino', 3.80, 'Beverages', 28),
                        ('P003', 'Croissant', 2.50, 'Bakery', 15),
                        ('P004', 'Cheesecake Slice', 4.50, 'Desserts', 12)
                """);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Sample data insert failed", e);
            }
        }
    }

    private static void runMigrations(Connection connection) {
        String[] migrations = {
                "ALTER TABLE orders ADD COLUMN payment_method TEXT DEFAULT 'CASH'",
                "ALTER TABLE order_items ADD COLUMN product_name TEXT",
                "ALTER TABLE order_items ADD COLUMN subtotal REAL",
                "ALTER TABLE transaction_sync_queue ADD COLUMN next_attempt_at DATETIME DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE products ADD COLUMN low_stock_threshold INTEGER DEFAULT 10",
                "ALTER TABLE products ADD COLUMN is_weighted INTEGER DEFAULT 0",
                "ALTER TABLE products ADD COLUMN unit TEXT DEFAULT 'each'",
                "ALTER TABLE products ADD COLUMN allergens TEXT DEFAULT ''"
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