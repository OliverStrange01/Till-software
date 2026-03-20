package com.till.controller;

import com.till.service.CartService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private SplitPane splitPane;
    @FXML private TextField cashField;
    @FXML private Label resultLabel;

    private final CartService cartService = new CartService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProductsPane();
        loadCartPane();
    }

    private void loadProductsPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/products-pane.fxml"));
            VBox productsPane = loader.load();
            ProductCategoryController controller = loader.getController();
            controller.setCartService(cartService);
            splitPane.getItems().add(productsPane);
            // Do NOT set divider here yet
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCartPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cart-pane.fxml"));
            VBox cartPane = loader.load();
            CartController controller = loader.getController();
            controller.setCartService(cartService);
            splitPane.getItems().add(cartPane);

            // Now both items exist → set initial divider position (55% for products)
            Platform.runLater(() -> {
                splitPane.setDividerPositions(0.60);  // 0.60 = products ~60%, cart ~40%
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCashPayment() {
        try {
            double cashGiven = Double.parseDouble(cashField.getText());
            double total = cartService.getTotal();
            double change = cartService.calculateChange(cashGiven);

            if (change < 0) {
                resultLabel.setText("Not enough cash. Total is £" + String.format("%.2f", total));
                resultLabel.setStyle("-fx-text-fill: #d32f2f;");
            } else {
                resultLabel.setText("Change: £" + String.format("%.2f", change));
                resultLabel.setStyle("-fx-text-fill: #2e7d32;");
                cartService.clearCart();
                cashField.clear();
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Please enter a valid amount.");
            resultLabel.setStyle("-fx-text-fill: #d32f2f;");
        }

    }

    @FXML
    private void openAdminStock() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin-stock.fxml"));
            VBox adminPane = loader.load();

            // Show in a new window (modal dialog)
            Stage adminStage = new Stage();
            adminStage.setTitle("Admin - Manage Stock");
            adminStage.setScene(new Scene(adminPane, 900, 600));
            adminStage.initOwner(splitPane.getScene().getWindow()); // center on main window
            adminStage.initModality(Modality.APPLICATION_MODAL);   // block main window
            adminStage.showAndWait();

            // Optional: refresh products after closing admin
            // You can call a refresh method on ProductCategoryController if needed
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open admin panel").showAndWait();
        }
    }
}