package com.till.dao;

import com.till.database.DatabaseConnection;
import com.till.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDAO {
    private static final Logger LOGGER = Logger.getLogger(ProductDAO.class.getName());

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, name, price, category, stock, barcode, low_stock_threshold, is_weighted, unit, allergens FROM products ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("category"),
                        rs.getInt("stock")
                );
                p.setBarcode(rs.getString("barcode") != null ? rs.getString("barcode") : "");
                list.add(p);
                p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                p.setWeighted(rs.getInt("is_weighted") == 1);
                p.setUnit(rs.getString("unit"));
                p.setAllergens(rs.getString("allergens") != null ? rs.getString("allergens") : "");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to load products", e);
        }
        return list;
    }

    public List<Product> getLowStockProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, name, price, category, stock, barcode, low_stock_threshold " +
                "FROM products WHERE stock <= low_stock_threshold ORDER BY stock ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("category"),
                        rs.getInt("stock")
                );
                p.setBarcode(rs.getString("barcode") != null ? rs.getString("barcode") : "");
                p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                list.add(p);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to load low stock products", e);
        }

        return list;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND category != '' ORDER BY category";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to load categories", e);
        }
        return categories;
    }

    public Product getProductById(String id) {
        String sql = "SELECT id, name, price, category, stock, barcode, low_stock_threshold, is_weighted, unit, allergens FROM products WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("category"),
                            rs.getInt("stock")
                    );
                    p.setBarcode(rs.getString("barcode") != null ? rs.getString("barcode") : "");
                    p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                    p.setWeighted(rs.getInt("is_weighted") == 1);
                    p.setUnit(rs.getString("unit"));
                p.setAllergens(rs.getString("allergens") != null ? rs.getString("allergens") : "");
                    return p;

                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to fetch product by id: " + id, e);
        }
        return null;
    }

    public Product findByBarcode(String barcode) {
        String sql = "SELECT id, name, price, category, stock, barcode, low_stock_threshold, is_weighted, unit, allergens FROM products WHERE barcode = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("category"),
                            rs.getInt("stock")
                    );
                    p.setBarcode(rs.getString("barcode") != null ? rs.getString("barcode") : "");
                    p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                    p.setWeighted(rs.getInt("is_weighted") == 1);
                    p.setUnit(rs.getString("unit"));
                p.setAllergens(rs.getString("allergens") != null ? rs.getString("allergens") : "");
                    return p;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to fetch product by barcode: " + barcode, e);
        }
        return null;
    }

    public void updateStock(String productId, int newStock) {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newStock);
            ps.setString(2, productId);
            ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to update stock for product: " + productId, e);
        }
    }

    public void updateBarcode(String productId, String barcode) {
        String sql = "UPDATE products SET barcode = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barcode);
            ps.setString(2, productId);
            ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to update barcode for product: " + productId, e);
        }
    }

    public void updateAllergens(String productId, String allergens) {
        String sql = "UPDATE products SET allergens = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, allergens != null ? allergens : "");
            ps.setString(2, productId);
            ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to update allergens for product: " + productId, e);
        }
    }
}
