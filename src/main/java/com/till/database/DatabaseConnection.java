package com.till.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String DB_PATH;
    private static final String URL;

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

    private static Connection connection;

    public static String getDbPath() {
        return DB_PATH;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
            }
            initTables();
        }
        return connection;
    }

    private static void initTables() {
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(products);
            stmt.execute(orders);
            stmt.execute(orderItems);
            System.out.println("Database tables ready");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        runMigrations();

        // Test data — remove when done
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
            System.err.println("Test data insert failed: " + e.getMessage());
        }
    }

    private static void runMigrations() {
        String[] migrations = {
                "ALTER TABLE orders ADD COLUMN payment_method TEXT DEFAULT 'CASH'",
                "ALTER TABLE order_items ADD COLUMN product_name TEXT",
                "ALTER TABLE order_items ADD COLUMN subtotal REAL"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String migration : migrations) {
                try {
                    stmt.execute(migration);
                    System.out.println("Migration applied: " + migration);
                } catch (SQLException e) {
                    System.out.println("Skipping migration (already applied): " + migration);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}