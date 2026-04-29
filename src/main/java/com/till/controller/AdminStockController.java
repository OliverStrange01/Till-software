package com.till.controller;

import com.till.dao.ProductDAO;
import com.till.database.DatabaseConnection;
import com.till.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminStockController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(AdminStockController.class.getName());

    @FXML private TableView<Product> stockTable;
    @FXML private TableColumn<Product, String> idCol;
    @FXML private TableColumn<Product, String> nameCol;
    @FXML private TableColumn<Product, Number> priceCol;
    @FXML private TableColumn<Product, Integer> currentStockCol;
    @FXML private TableColumn<Product, Integer> addStockCol;
    @FXML private TableColumn<Product, String> barcodeCol;
    @FXML private TableColumn<Product, String> allergensCol;

    @FXML private Label statusLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private ObservableList<Product> products = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        refreshStock();
    }

    private static final List<String> COMMON_CATEGORIES = List.of(
            "Beverages",
            "Bakery",
            "Snacks",
            "Desserts",
            "Sandwiches",
            "Hot Food",
            "Cold Drinks",
            "Alcohol",
            "Tobacco",
            "Miscellaneous"
    );

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        currentStockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        currentStockCol.setEditable(false);

        addStockCol.setCellValueFactory(new PropertyValueFactory<>("stockToAdd"));
        addStockCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        addStockCol.setOnEditCommit(event -> {
            Product product = event.getRowValue();
            Integer newDelta = event.getNewValue();

            if (newDelta == null) {
                newDelta = 0;
            }

            product.setStockToAdd(newDelta);

            if (newDelta != 0) {
                String sign = newDelta > 0 ? "+" : "";
                statusLabel.setText(String.format(
                        "Change queued for %s: %s%d (new stock would be %d)",
                        product.getName(), sign, newDelta, product.getStock() + newDelta
                ));
            } else {
                statusLabel.setText("Change cleared for " + product.getName());
            }

            stockTable.refresh();
        });

        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        barcodeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        barcodeCol.setOnEditCommit(event -> {
            Product product = event.getRowValue();
            product.setBarcode(event.getNewValue().trim());
            productDAO.updateBarcode(product.getId(), product.getBarcode());
            statusLabel.setText("Barcode updated for " + product.getName());
        });

        allergensCol.setCellValueFactory(new PropertyValueFactory<>("allergens"));
        allergensCol.setCellFactory(TextFieldTableCell.forTableColumn());
        allergensCol.setOnEditCommit(event -> {
            Product product = event.getRowValue();
            product.setAllergens(event.getNewValue().trim());
            productDAO.updateAllergens(product.getId(), product.getAllergens());
            statusLabel.setText("Allergens updated for " + product.getName());
        });

        stockTable.setItems(products);
        stockTable.setEditable(true);
        stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void refreshStock() {
        List<Product> loaded = productDAO.getAllProducts();

        for (Product p : loaded) {
            String original = p.getCategory();
            String normalized = normalizeCategory(original);
            if (!normalized.equals(original)) {
                p.setCategory(normalized);
            }
            p.setStockToAdd(0);
        }

        products.setAll(loaded);

        if (products.isEmpty()) {
            statusLabel.setText("No products found in database");
        } else {
            statusLabel.setText("Stock list refreshed (" + products.size() + " products loaded)");
        }
    }
    @FXML
    private void showLowStockOnly() {
        List<Product> lowStock = productDAO.getLowStockProducts();

        for (Product p : lowStock) {
            p.setStockToAdd(0);
        }

        products.setAll(lowStock);

        if (products.isEmpty()) {
            statusLabel.setText("No low stock products found");
        } else {
            statusLabel.setText("Showing " + products.size() + " low stock product(s)");
        }
    }

    @FXML
    private void saveStockChanges() {
        int updatedCount = 0;
        int blockedCount = 0;

        for (Product product : products) {
            int change = product.getStockToAdd();
            if (change == 0) continue;

            int newStock = product.getStock() + change;

            if (newStock < 0) {
                statusLabel.setText("Blocked: " + product.getName() + " would go negative (" + newStock + ")");
                blockedCount++;
                product.setStockToAdd(0);
                continue;
            }

            productDAO.updateStock(product.getId(), newStock);
            product.setStock(newStock);
            product.setStockToAdd(0);
            updatedCount++;
        }

        String msg = "";
        if (updatedCount > 0) {
            msg += "Saved " + updatedCount + " change" + (updatedCount == 1 ? "" : "s");
        }
        if (blockedCount > 0) {
            if (!msg.isEmpty()) msg += " • ";
            msg += blockedCount + " blocked (would be negative)";
        }
        if (msg.isEmpty()) {
            msg = "No changes to save";
        }

        statusLabel.setText(msg);
        stockTable.refresh();
    }

    @FXML
    private void addNewProduct() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter product details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("Unique ID (e.g. BANANA01)");
        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);

        TextField nameField = new TextField();
        nameField.setPromptText("Product name");
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        TextField priceField = new TextField("0.00");
        priceField.setPromptText("Price");
        grid.add(new Label("Price £:"), 0, 2);
        grid.add(priceField, 1, 2);

        TextField stockField = new TextField("0");
        stockField.setPromptText("Initial stock");
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        categoryCombo.getItems().addAll(COMMON_CATEGORIES);
        categoryCombo.setValue("Miscellaneous");
        categoryCombo.setPromptText("Select or type category");
        grid.add(new Label("Category:"), 0, 4);
        grid.add(categoryCombo, 1, 4);

        TextField barcodeField = new TextField();
        barcodeField.setPromptText("Barcode (optional)");
        grid.add(new Label("Barcode:"), 0, 5);
        grid.add(barcodeField, 1, 5);
        CheckBox weightedBox = new CheckBox("Sold by weight");
        grid.add(new Label("Weighted:"), 0, 6);
        grid.add(weightedBox, 1, 6);

        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll("each", "kg", "g");
        unitCombo.setValue("each");
        grid.add(new Label("Unit:"), 0, 7);
        grid.add(unitCombo, 1, 7);

        TextField allergensField = new TextField();
        allergensField.setPromptText("e.g. Gluten, Milk, Nuts");
        grid.add(new Label("Allergens:"), 0, 8);
        grid.add(allergensField, 1, 8);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String id   = idField.getText().trim();
                    String name = nameField.getText().trim();

                    double price;
                    try {
                        price = Double.parseDouble(priceField.getText().trim());
                    } catch (NumberFormatException e) {
                        showAlert("Invalid price format.");
                        return null;
                    }

                    int stock;
                    try {
                        stock = Integer.parseInt(stockField.getText().trim());
                    } catch (NumberFormatException e) {
                        showAlert("Invalid stock value.");
                        return null;
                    }

                    String rawCat = categoryCombo.getValue() != null
                            ? categoryCombo.getValue().trim()
                            : "Miscellaneous";

                    if (id.isEmpty() || name.isEmpty()) {
                        showAlert("ID and Name are required.");
                        return null;
                    }

                    String normalizedCat = normalizeCategory(rawCat);
                    Product product = new Product(id, name, price, normalizedCat, stock);
                    product.setBarcode(barcodeField.getText().trim());
                    product.setWeighted(weightedBox.isSelected());
                    product.setUnit(unitCombo.getValue());
                    product.setAllergens(allergensField.getText().trim());
                    return product;

                } catch (Exception e) {
                    showAlert("Error creating product: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            try {
                insertProduct(product);
                refreshStock();
                statusLabel.setText("Added: " + product.getName() + " (" + product.getCategory() + ")");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Unable to add new product", e);
                showAlert("Failed to add product.\n" + e.getMessage());
            }
        });
    }

    private void insertProduct(Product p) throws SQLException {
        String finalCategory = normalizeCategory(p.getCategory());
        p.setCategory(finalCategory);

        String sql = "INSERT INTO products (id, name, price, category, stock, barcode, is_weighted, unit, allergens) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getId());
            ps.setString(2, p.getName());
            ps.setDouble(3, p.getPrice());
            ps.setString(4, finalCategory);
            ps.setInt(5, p.getStock());
            ps.setString(6, p.getBarcode());
            ps.setInt(7, p.isWeighted() ? 1 : 0);
            ps.setString(8, p.getUnit());
            ps.setString(9, p.getAllergens());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed – ID may already exist");
            }
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Miscellaneous";
        }

        String trimmed = category.trim();
        trimmed = trimmed.replaceAll("\\s+", " ");

        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : trimmed.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb.toString();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) stockTable.getScene().getWindow();
        stage.close();
    }
}