package com.till.controller;

import com.till.dao.ProductDAO;
import com.till.dao.SalesDAO;          // ← add this import
import com.till.model.OrderItem;
import com.till.model.Product;
import com.till.model.SalesRecord;     // ← add this import
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    @FXML private Button endOfDayButton;

    private final CartService cartService = new CartService();
    private final ProductDAO productDAO = new ProductDAO();
    private final SalesDAO salesDAO = new SalesDAO();   // ← add this

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProductsPane();
        loadCartPane();
        adminButton.setVisible(false);
        endOfDayButton.setVisible(false);
    }

    public void setAdminMode(boolean isAdmin) {
        adminButton.setVisible(isAdmin);
        endOfDayButton.setVisible(isAdmin);
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
            cartService.clearCart();
            Stage current = (Stage) splitPane.getScene().getWindow();
            current.close();

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

    @FXML
    private void handleCashPayment() {
        if (cartService.getCartItems().isEmpty()) {
            resultLabel.setText("Basket is empty");
            resultLabel.setStyle("-fx-text-fill: orange;");
            return;
        }
        if (!validateStock()) return;  // ← add this

        try {
            double cashGiven = Double.parseDouble(cashField.getText().trim());
            double total = cartService.getTotal();
            double change = total > 0 ? cashGiven - total : 0;

            if (cashGiven < total) {
                resultLabel.setText("Not enough cash. Total is £" + String.format("%.2f", total));
                resultLabel.setStyle("-fx-text-fill: #d32f2f;");
                return;
            }
            processSuccessfulPayment(total, cashGiven, change, true);
        } catch (NumberFormatException e) {
            resultLabel.setText("Please enter a valid amount.");
            resultLabel.setStyle("-fx-text-fill: #d32f2f;");
        }
    }

    @FXML
    private void handleCardPayment() {
        if (cartService.getCartItems().isEmpty()) {
            resultLabel.setText("Basket is empty");
            resultLabel.setStyle("-fx-text-fill: orange;");
            return;
        }
        if (!validateStock()) return;  // ← add this

        double total = cartService.getTotal();

        if (Math.random() < 0.1) {
            resultLabel.setText("Card declined – Try again or use cash");
            resultLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        processSuccessfulPayment(total, total, 0, false);
    }

    private void processSuccessfulPayment(double total, double tendered, double change, boolean isCash) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Accepted");
        alert.setHeaderText(isCash ? "Paid with Cash" : "Paid with Card");
        alert.setContentText(
                "Total: £" + String.format("%.2f", total) + "\n" +
                        (isCash ? "Cash Given: £" + String.format("%.2f", tendered) + "\nChange: £" + String.format("%.2f", change) : "No change required") + "\n\n" +
                        "Thank you!"
        );
        alert.showAndWait();

        generatePdfReceipt(total, tendered, change, isCash);   // PDF before clear
        reduceStock();                                          // stock before clear
        salesDAO.logTransaction(                               // ← fixed: instance call
                new ArrayList<>(cartService.getCartItems()),
                total,
                isCash ? "CASH" : "CARD"
        );

        cartService.clearCart();                               // clear last
        cashField.clear();
        resultLabel.setText("Payment completed");
        resultLabel.setStyle("-fx-text-fill: green;");
    }

    private boolean validateStock() {
        for (OrderItem item : cartService.getCartItems()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                resultLabel.setText("Not enough stock for: " + item.getProduct().getName() +
                        " (requested " + item.getQuantity() + ", available " + item.getProduct().getStock() + ")");
                resultLabel.setStyle("-fx-text-fill: #d32f2f;");
                return false;
            }
        }
        return true;
    }

    private void reduceStock() {
        for (OrderItem item : cartService.getCartItems()) {
            Product p = item.getProduct();
            int newStock = p.getStock() - item.getQuantity();
            if (newStock < 0) newStock = 0;
            productDAO.updateStock(p.getId(), newStock);
        }
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
            Runtime.getRuntime().exec("cmd /c start " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to create PDF receipt").showAndWait();
        }
    }

    @FXML
    private void handleEndOfDay() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("End of Day");
        confirm.setHeaderText("Generate end of day report?");
        confirm.setContentText("This will generate a PDF summary of today's sales.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        List<SalesRecord> breakdown = salesDAO.getTodayBreakdown();
        double total = salesDAO.getTodayTotal();
        int txCount = salesDAO.getTodayTransactionCount();

        if (txCount == 0) {
            new Alert(Alert.AlertType.INFORMATION, "No transactions recorded today.").showAndWait();
            return;
        }

        generateEndOfDayPdf(breakdown, total, txCount);

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("End of Day Complete");
        done.setContentText("Report generated. Total sales today: £" + String.format("%.2f", total));
        done.showAndWait();
    }

    private void generateEndOfDayPdf(List<SalesRecord> breakdown, double total, int txCount) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String filePath = "reports/end_of_day_" + date + ".pdf";

        try {
            new File("reports").mkdirs();

            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("End of Day Report").setBold().setFontSize(20));
            document.add(new Paragraph("Date: " + date));
            document.add(new Paragraph("Total Transactions: " + txCount));
            document.add(new Paragraph("----------------------------------------"));

            document.add(new Paragraph("Product Breakdown:").setBold());
            for (SalesRecord record : breakdown) {
                document.add(new Paragraph(
                        String.format("%-25s %3d sold   £%.2f",
                                record.getProductName(),
                                record.getQuantitySold(),
                                record.getRevenue())
                ));
            }

            document.add(new Paragraph("----------------------------------------"));
            document.add(new Paragraph(String.format("Total Revenue: £%.2f", total)).setBold());
            document.close();

            Runtime.getRuntime().exec("cmd /c start " + filePath);
            System.out.println("End of day report saved: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to generate end of day report").showAndWait();
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