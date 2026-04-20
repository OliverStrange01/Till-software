package com.till.controller;

import com.till.config.AppConfig;
import com.till.dao.AuditDAO;
import com.till.dao.ProductDAO;
import com.till.dao.SalesDAO;
import com.till.dao.TransactionSyncDAO;
import com.till.model.AssistanceCall;
import com.till.model.AssistanceType;
import com.till.model.Coupon;
import com.till.model.DiscountApplicationResult;
import com.till.model.OrderItem;
import com.till.model.Product;
import com.till.model.QueuedTransaction;
import com.till.model.ReceiptData;
import com.till.model.SalesRecord;
import com.till.model.TransactionPayload;
import com.till.service.AssistanceService;
import com.till.service.CartService;
import com.till.service.DiscountService;
import com.till.service.ReceiptService;
import com.till.service.TransactionApiClient;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
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
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML private SplitPane splitPane;
    @FXML private TextField cashField;
    @FXML private TextField couponField;
    @FXML private Label resultLabel;
    @FXML private Button adminButton;
    @FXML private Button endOfDayButton;
    @FXML private Button auditButton;

    private final CartService cartService = new CartService();
    private final ProductDAO productDAO = new ProductDAO();
    private final SalesDAO salesDAO = new SalesDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final TransactionSyncDAO transactionSyncDAO = new TransactionSyncDAO();
    private final DiscountService discountService = new DiscountService();
    private final AssistanceService assistanceService = new AssistanceService();
    private final ReceiptService receiptService = new ReceiptService();
    private final TransactionApiClient transactionApiClient = new TransactionApiClient(
            AppConfig.transactionApiUrl(),
            AppConfig.transactionApiKey()
    );

    // Keep active discount state in-memory for the current basket only.
    private Coupon appliedCoupon;
    private double appliedDiscountAmount;
    private boolean adminMode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProductsPane();
        loadCartPane();
        adminButton.setVisible(false);
        endOfDayButton.setVisible(false);
        auditButton.setVisible(false);
        processPendingSyncQueue();
    }

    public void setAdminMode(boolean isAdmin) {
        this.adminMode = isAdmin;
        adminButton.setVisible(isAdmin);
        endOfDayButton.setVisible(isAdmin);
        auditButton.setVisible(isAdmin);
    }

    private void loadProductsPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/products-pane.fxml"));
            VBox productsPane = loader.load();
            ProductCategoryController controller = loader.getController();
            controller.setCartService(cartService);
            splitPane.getItems().add(productsPane);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load products pane", e);
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
            LOGGER.log(Level.SEVERE, "Failed to load cart pane", e);
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
                LOGGER.log(Level.SEVERE, "Failed to open login screen", e);
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
        if (!validateStock()) return;

        try {
            double cashGiven = Double.parseDouble(cashField.getText().trim());
            double total = calculateFinalTotal();
            double change = total > 0 ? cashGiven - total : 0;

            if (cashGiven < total) {
                resultLabel.setText("Not enough cash. Total is £" + String.format(Locale.UK, "%.2f", total));
                resultLabel.setStyle("-fx-text-fill: #d32f2f;");
                return;
            }
            processSuccessfulPayment(total, cashGiven, change, "CASH");
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
        if (!validateStock()) return;

        double total = calculateFinalTotal();

        if (Math.random() < 0.1) {
            resultLabel.setText("Card declined – Try again or use cash");
            resultLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        processSuccessfulPayment(total, total, 0, "CARD");
    }

    @FXML
    private void handleApplyCoupon() {
        if (cartService.getCartItems().isEmpty()) {
            resultLabel.setText("Add items to the basket before applying a coupon.");
            resultLabel.setStyle("-fx-text-fill: #d32f2f;");
            return;
        }
        double subTotal = cartService.getTotal();
        DiscountApplicationResult result = discountService.applyCoupon(couponField.getText(), subTotal, appliedCoupon);
        if (!result.isSuccess()) {
            resultLabel.setText(result.getMessage());
            resultLabel.setStyle("-fx-text-fill: #d32f2f;");
            auditDAO.logEvent("COUPON_REJECTED", result.getMessage(), adminMode ? "ADMIN" : "CASHIER");
            return;
        }

        appliedCoupon = result.getCoupon();
        appliedDiscountAmount = result.getDiscountAmount();
        resultLabel.setText(result.getMessage() + " | New total: £" + String.format(Locale.UK, "%.2f", result.getFinalTotal()));
        resultLabel.setStyle("-fx-text-fill: #2e7d32;");
        auditDAO.logEvent("COUPON_APPLIED", result.getMessage(), adminMode ? "ADMIN" : "CASHIER");
    }

    private void processSuccessfulPayment(double total, double tendered, double change, String paymentMethod) {
        if (!checkAssistanceCalls(total)) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Accepted");
        alert.setHeaderText("CASH".equals(paymentMethod) ? "Paid with Cash" : "Paid with Card");
        alert.setContentText(
                "Total: £" + String.format(Locale.UK, "%.2f", total) + "\n" +
                        ("CASH".equals(paymentMethod) ? "Cash Given: £" + String.format(Locale.UK, "%.2f", tendered) + "\nChange: £" + String.format(Locale.UK, "%.2f", change) : "No change required") + "\n\n" +
                        "Thank you!"
        );
        alert.showAndWait();

        generatePdfReceipt(total, tendered, change, paymentMethod);
        reduceStock();
        salesDAO.logTransaction(
                new ArrayList<>(cartService.getCartItems()),
                total,
                paymentMethod
        );
        TransactionPayload payload = new TransactionPayload(
                new ArrayList<>(cartService.getCartItems()),
                cartService.getTotal(),
                appliedDiscountAmount,
                total,
                paymentMethod,
                appliedCoupon == null ? null : appliedCoupon.getCode()
        );
        syncTransactionPayload(payload);
        auditDAO.logEvent(
                "PAYMENT_COMPLETED",
                "Method=" + paymentMethod + ", total=" + String.format(Locale.UK, "%.2f", total),
                adminMode ? "ADMIN" : "CASHIER"
        );

        cartService.clearCart();
        appliedCoupon = null;
        appliedDiscountAmount = 0;
        if (couponField != null) {
            couponField.clear();
        }
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

    private void generatePdfReceipt(double total, double tendered, double change, String paymentMethod) {
        try {
            String filePath = receiptService.generateReceiptPdf(new ReceiptData(
                    new ArrayList<>(cartService.getCartItems()),
                    cartService.getTotal(),
                    appliedDiscountAmount,
                    total,
                    tendered,
                    change,
                    paymentMethod,
                    appliedCoupon == null ? null : appliedCoupon.getCode(),
                    LocalDateTime.now()
            ));
            Runtime.getRuntime().exec("cmd /c start " + filePath);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create or open PDF receipt", e);
            new Alert(Alert.AlertType.ERROR, "Failed to create PDF receipt").showAndWait();
        }
    }

    private double calculateFinalTotal() {
        return Math.max(0, cartService.getTotal() - appliedDiscountAmount);
    }

    private boolean checkAssistanceCalls(double finalTotal) {
        // Assistance checks are deliberately centralised here so every payment path
        // (cash or card) applies the same operational safeguards.
        List<AssistanceCall> calls = assistanceService.evaluateAssistanceNeeds(
                new ArrayList<>(cartService.getCartItems()),
                finalTotal,
                appliedCoupon != null
        );
        if (calls.isEmpty()) {
            return true;
        }

        StringBuilder message = new StringBuilder("Staff assistance needed:\n\n");
        for (AssistanceCall call : calls) {
            message.append("• ").append(call.getType()).append(": ").append(call.getReason()).append("\n");
            auditDAO.logEvent("ASSISTANCE_CALL", call.getType() + ": " + call.getReason(), adminMode ? "ADMIN" : "CASHIER");
        }

        boolean managerOverrideRequested = calls.stream().anyMatch(c -> c.getType() == AssistanceType.MANAGER_OVERRIDE);
        if (managerOverrideRequested && !adminMode) {
            resultLabel.setText("Manager override required before payment can continue.");
            resultLabel.setStyle("-fx-text-fill: #d32f2f;");
            new Alert(Alert.AlertType.WARNING, message.toString()).showAndWait();
            return false;
        }

        Alert review = new Alert(Alert.AlertType.CONFIRMATION);
        review.setTitle("Assistance Check");
        review.setHeaderText("Please confirm assistance checks");
        review.setContentText(message + "\nContinue with payment?");
        return review.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
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
        done.setContentText("Report generated. Total sales today: £" + String.format(Locale.UK, "%.2f", total));
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
                        String.format(Locale.UK, "%-25s %3d sold   £%.2f",
                                record.getProductName(),
                                record.getQuantitySold(),
                                record.getRevenue())
                ));
            }

            document.add(new Paragraph("----------------------------------------"));
            document.add(new Paragraph(String.format(Locale.UK, "Total Revenue: £%.2f", total)).setBold());
            document.close();

            Runtime.getRuntime().exec("cmd /c start " + filePath);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to generate end of day report", e);
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
            LOGGER.log(Level.WARNING, "Failed to open admin stock window", e);
            new Alert(Alert.AlertType.ERROR, "Failed to open admin panel").showAndWait();
        }
    }

    @FXML
    private void openAuditQueue() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/audit-queue.fxml"));
            VBox pane = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Audit and Queue Monitor");
            stage.setScene(new Scene(pane, 1000, 700));
            stage.initOwner(splitPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to open audit/queue monitor", e);
            new Alert(Alert.AlertType.ERROR, "Failed to open audit/queue monitor").showAndWait();
        }
    }

    private void syncTransactionPayload(TransactionPayload payload) {
        String payloadJson = transactionApiClient.serialisePayload(payload);
        boolean synced = transactionApiClient.submitJson(payloadJson);
        if (!synced) {
            transactionSyncDAO.enqueue(payloadJson);
            auditDAO.logEvent("API_SYNC_QUEUED", "Transaction sync queued for retry", adminMode ? "ADMIN" : "CASHIER");
            LOGGER.warning("Transaction API sync failed; queued for retry");
        } else {
            auditDAO.logEvent("API_SYNC_SUCCESS", "Transaction synced successfully", adminMode ? "ADMIN" : "CASHIER");
        }
    }

    private void processPendingSyncQueue() {
        List<QueuedTransaction> pending = transactionSyncDAO.getPending(AppConfig.transactionSyncBatchSize());
        for (QueuedTransaction queuedTransaction : pending) {
            boolean synced = transactionApiClient.submitJson(queuedTransaction.getPayloadJson());
            if (synced) {
                transactionSyncDAO.markSynced(queuedTransaction.getId());
            } else {
                transactionSyncDAO.markFailed(queuedTransaction.getId(), "Sync failed at startup retry");
            }
        }
    }

}