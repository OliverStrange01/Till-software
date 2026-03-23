package com.till.controller;

import com.till.dao.ProductDAO;
import com.till.model.OrderItem;
import com.till.model.Product;
import com.till.service.CartService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class MainController implements Initializable {

    @FXML private SplitPane splitPane;
    @FXML private TextField cashField;
    @FXML private Label resultLabel;
    @FXML private Button adminButton;

    private final CartService cartService = new CartService();
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProductsPane();
        loadCartPane();
        adminButton.setVisible(false);
    }

    public void setAdminMode(boolean isAdmin) {
        adminButton.setVisible(isAdmin);
    }

    private void loadProductsPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/products-pane.fxml"));
            VBox productsPane = loader.load();
            ProductCategoryController controller = loader.getController();
            controller.setCartService(cartService);
            splitPane.getItems().add(productsPane);
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

            Platform.runLater(() -> splitPane.setDividerPositions(0.60));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Return to login?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            cartService.clearCart();  // optional
            Stage current = (Stage) splitPane.getScene().getWindow();
            current.close();

            // Re-open login
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Scene scene = new Scene(loader.load(), 500, 400);
                Stage loginStage = new Stage();
                loginStage.setTitle("Till POS - Login");
                loginStage.setScene(scene);
                loginStage.setResizable(false);
                loginStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Existing cash payment
    @FXML
    private void handleCashPayment() {
        if (cartService.getCartItems().isEmpty()) {
            resultLabel.setText("Basket is empty");
            resultLabel.setStyle("-fx-text-fill: orange;");
            return;
        }
        try {
            double cashGiven = Double.parseDouble(cashField.getText().trim());
            double total = cartService.getTotal();
            double change = total > 0 ? cashGiven - total : 0;

            if (cashGiven < total) {
                resultLabel.setText("Not enough cash. Total is £" + String.format("%.2f", total));
                resultLabel.setStyle("-fx-text-fill: #d32f2f;");
                return;
            }

            // Success path
            processSuccessfulPayment(total, cashGiven, change, true);
        } catch (NumberFormatException e) {
            resultLabel.setText("Please enter a valid amount.");
            resultLabel.setStyle("-fx-text-fill: #d32f2f;");
        }
    }

    // New: Pay with Card
    @FXML
    private void handleCardPayment() {
        double total = cartService.getTotal();
        if (total <= 0) {
            resultLabel.setText("Cart is empty");
            resultLabel.setStyle("-fx-text-fill: orange;");
            return;
        }

        // Simulate 10% decline chance
        if (Math.random() < 0.1) {
            resultLabel.setText("Card declined – Try again or use cash");
            resultLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Success path
        processSuccessfulPayment(total, total, 0, false);
    }

    // Shared success logic
    private void processSuccessfulPayment(double total, double tendered, double change, boolean isCash) {
        // Show accepted prompt
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Accepted");
        alert.setHeaderText(isCash ? "Paid with Cash" : "Paid with Card");
        alert.setContentText(
                "Total: £" + String.format("%.2f", total) + "\n" +
                        (isCash ? "Cash Given: £" + String.format("%.2f", tendered) + "\nChange: £" + String.format("%.2f", change) : "No change required") + "\n\n" +
                        "Thank you!"
        );
        alert.showAndWait();

        // Reduce stock
        reduceStock();

        // Generate PDF receipt
        generatePdfReceipt(total, tendered, change, isCash);

        // Clear cart
        cartService.clearCart();
        cashField.clear();
        resultLabel.setText("Payment completed");
        resultLabel.setStyle("-fx-text-fill: green;");
    }

    private void reduceStock() {
        for (OrderItem item : cartService.getCartItems()) {
            Product p = item.getProduct();
            int newStock = p.getStock() - item.getQuantity();
            if (newStock < 0) newStock = 0;
            productDAO.updateStock(p.getId(), newStock);
        }
        System.out.println("Stock reduced for cart items");
    }

    private void generatePdfReceipt(double total, double tendered, double change, boolean isCash) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filePath = "receipts/receipt_" + timestamp + ".pdf";

        try {
            new File("receipts").mkdirs();

            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Till POS Receipt").setBold().setFontSize(18));
            document.add(new Paragraph("Date: " + LocalDateTime.now()));
            document.add(new Paragraph("----------------------------------------"));

            for (OrderItem item : cartService.getCartItems()) {
                document.add(new Paragraph(
                        String.format("%-25s %3d x £%.2f = £%.2f",
                                item.getProduct().getName(), item.getQuantity(),
                                item.getProduct().getPrice(), item.getSubtotal())
                ));
            }

            document.add(new Paragraph("----------------------------------------"));
            document.add(new Paragraph(String.format("Total: £%.2f", total)));
            if (isCash) {
                document.add(new Paragraph(String.format("Cash: £%.2f", tendered)));
                document.add(new Paragraph(String.format("Change: £%.2f", change)));
            } else {
                document.add(new Paragraph("Paid by Card"));
            }
            document.add(new Paragraph("Thank you!"));

            document.close();
            System.out.println("Receipt PDF saved: " + filePath);

            // Auto-open on Windows
            Runtime.getRuntime().exec("cmd /c start " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to create PDF receipt").showAndWait();
        }
    }

    @FXML
    private void openAdminStock() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin-stock.fxml"));
            VBox adminPane = loader.load();

            Stage adminStage = new Stage();
            adminStage.setTitle("Admin - Manage Stock");
            adminStage.setScene(new Scene(adminPane, 900, 600));
            adminStage.initOwner(splitPane.getScene().getWindow());
            adminStage.initModality(Modality.APPLICATION_MODAL);
            adminStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open admin panel").showAndWait();
        }
    }
}