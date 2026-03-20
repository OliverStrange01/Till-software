package com.till.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {


    private static final String DB_PATH = "pos_system.db"; // creates in project root
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private static Connection connection;


    public static String getDbPath() {
        return DB_PATH;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            // Optional: enable WAL mode for better concurrency
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
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String order_items = """
            CREATE TABLE IF NOT EXISTS order_items (
                order_id INTEGER,
                product_id TEXT,
                quantity INTEGER,
                price REAL,
                FOREIGN KEY(order_id) REFERENCES orders(order_id),
                FOREIGN KEY(product_id) REFERENCES products(id)
            )
        """;


        try (Statement stmt = connection.createStatement()) {
            stmt.execute(products);
            stmt.execute(orders);
            stmt.execute(order_items);
            System.out.println("Database tables ready");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // === Temporary test data – comment out or remove later ===
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
        INSERT OR IGNORE INTO products (id, name, price, category, stock)
        VALUES 
            ('P001', 'Espresso', 3.20, 'Beverages', 45),
            ('P002', 'Cappuccino', 3.80, 'Beverages', 28),
            ('P003', 'Croissant', 2.50, 'Bakery', 15),
            ('P004', 'Cheesecake Slice', 4.50, 'Desserts', 12)
    """);
            System.out.println("→ Inserted 4 test products");
        } catch (SQLException e) {
            System.err.println("Test data insert failed: " + e.getMessage());
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
