package com.till.dao;

import com.till.database.DatabaseConnection;
import com.till.model.OrderItem;
import com.till.model.SalesRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SalesDAO {
    private static final Logger LOGGER = Logger.getLogger(SalesDAO.class.getName());

    public void logTransaction(List<OrderItem> items, double total, String paymentMethod) {
        String insertOrder = """
            INSERT INTO orders (total, cash_given, change, payment_method)
            VALUES (?, ?, ?, ?)
        """;
        String insertItem = """
            INSERT INTO order_items (order_id, product_id, product_name, quantity, price, subtotal)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            long orderId;
            try (PreparedStatement orderStmt = conn.prepareStatement(
                    insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setDouble(1, total);
                orderStmt.setDouble(2, total);
                orderStmt.setDouble(3, 0);
                orderStmt.setString(4, paymentMethod);
                orderStmt.executeUpdate();

                try (ResultSet keys = orderStmt.getGeneratedKeys()) {
                    orderId = keys.next() ? keys.getLong(1) : -1;
                }
                if (orderId < 0) {
                    throw new SQLException("Failed to generate order id");
                }
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertItem)) {
                for (OrderItem item : items) {
                    itemStmt.setLong(1, orderId);
                    itemStmt.setString(2, item.getProduct().getId());
                    itemStmt.setString(3, item.getProduct().getName());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setDouble(5, item.getProduct().getPrice());
                    itemStmt.setDouble(6, item.getSubtotal());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to log transaction", e);
        }
    }

    public List<SalesRecord> getTodayBreakdown() {
        String sql = """
            SELECT product_name,
                   SUM(quantity)  AS total_qty,
                   SUM(subtotal)  AS total_revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.order_id
            WHERE DATE(o.timestamp) = ?
            GROUP BY product_name
            ORDER BY total_revenue DESC
        """;

        List<SalesRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDate.now().toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(new SalesRecord(
                        rs.getString("product_name"),
                        rs.getInt("total_qty"),
                        rs.getDouble("total_revenue")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to build today's sales breakdown", e);
        }
        return records;
    }

    public double getTodayTotal() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE DATE(timestamp) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDate.now().toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to fetch today's total", e);
        }
        return 0;
    }

    public int getTodayTransactionCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE DATE(timestamp) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDate.now().toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to fetch today's transaction count", e);
        }
        return 0;
    }
}