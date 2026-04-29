package com.till.controller;

import com.till.model.OrderItem;
import com.till.model.Product;
import com.till.service.CartService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML private TableView<OrderItem> cartTable;
    @FXML private TableColumn<OrderItem, String> nameCol;
    @FXML private TableColumn<OrderItem, Double> qtyCol;
    @FXML private TableColumn<OrderItem, Double> priceCol;
    @FXML private TableColumn<OrderItem, Double> subtotalCol;
    @FXML private Label totalLabel;
    @FXML private Button clearButton;

    private CartService cartService;

    public void setCartService(CartService service) {
        this.cartService = service;
        cartTable.setItems(cartService.getCartItems());
        totalLabel.textProperty().bind(
                Bindings.format("Total: £%.2f", cartService.totalBinding())
        );
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
    }

    private void setupTableColumns() {
        nameCol.setCellValueFactory(cellData ->
                cellData.getValue().getProduct().nameProperty()
        );
        // Show pencil icon if item has special instructions
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setTooltip(null); return; }
                OrderItem item = getTableView().getItems().get(getIndex());
                String note = item.getSpecialInstructions();
                setText(note != null && !note.isEmpty() ? v + " 📝" : v);
                setTooltip(note != null && !note.isEmpty() ? new Tooltip("Note: " + note) : null);
            }
        });

        // Qty column — show as decimal for weighted items, integer for normal ones
        qtyCol.setCellValueFactory(cellData ->
                cellData.getValue().quantityProperty().asObject()
        );
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setText(null); return; }
                OrderItem item = getTableView().getItems().get(getIndex());
                boolean weighted = item.getProduct().isWeighted();
                String unit = item.getProduct().getUnit();
                if (weighted && !"each".equalsIgnoreCase(unit)) {
                    setText(String.format("%.3f %s", value, unit));
                } else {
                    setText(String.valueOf(value.intValue()));
                }
            }
        });

        priceCol.setCellValueFactory(cellData ->
                cellData.getValue().getProduct().priceProperty().asObject()
        );
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("£%.2f", value));
            }
        });

        subtotalCol.setCellValueFactory(cellData ->
                cellData.getValue().subtotalProperty().asObject()
        );
        subtotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        subtotalCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("£%.2f", value));
            }
        });

        // Actions column: [ - ] [ X ] [ ✎ ]
        TableColumn<OrderItem, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(130);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button decrementBtn = new Button("-");
            private final Button removeBtn    = new Button("X");
            private final Button editBtn      = new Button("✎");
            private final HBox box = new HBox(4, decrementBtn, removeBtn, editBtn);

            {
                decrementBtn.setStyle("-fx-background-color: #f0a500; -fx-text-fill: white; -fx-font-weight: bold;");
                removeBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                editBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");

                decrementBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    cartService.decrementItem(item);
                });
                removeBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    cartService.removeItem(item);
                });
                editBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    showEditLineDialog(item);
                });
            }

            @Override
            protected void updateItem(Void unused, boolean empty) {
                super.updateItem(unused, empty);
                setGraphic(empty ? null : box);
            }
        });

        cartTable.getColumns().add(actionsCol);
    }

    // ── Edit Line / Special Instructions ─────────────────────────────────────
    private void showEditLineDialog(OrderItem item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Line");
        dialog.setHeaderText("Editing: " + item.getProduct().getName());

        boolean weighted = item.getProduct().isWeighted()
                && !"each".equalsIgnoreCase(item.getProduct().getUnit());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(15));

        TextField noteField = new TextField(
                item.getSpecialInstructions() != null ? item.getSpecialInstructions() : ""
        );
        noteField.setPromptText("e.g. No ice, extra hot, allergy note...");
        noteField.setPrefWidth(280);

        if (weighted) {
            // For weighted products: enter kg/g amount directly
            String unit = item.getProduct().getUnit();
            Spinner<Double> weightSpinner = new Spinner<>(0.001, 999.0, item.getQuantity(), 0.1);
            weightSpinner.setEditable(true);
            weightSpinner.setPrefWidth(100);
            grid.add(new Label("Quantity (" + unit + "):"), 0, 0);
            grid.add(weightSpinner, 1, 0);
            grid.add(new Label("Special Instructions:"), 0, 1);
            grid.add(noteField, 1, 1);
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    cartService.updateQuantity(item, weightSpinner.getValue());
                    item.setSpecialInstructions(noteField.getText().trim());
                    cartTable.refresh();
                }
                return null;
            });
        } else {
            Spinner<Integer> qtySpinner = new Spinner<>(1, 999, (int) item.getQuantity());
            qtySpinner.setEditable(true);
            qtySpinner.setPrefWidth(80);
            grid.add(new Label("Quantity:"), 0, 0);
            grid.add(qtySpinner, 1, 0);
            grid.add(new Label("Special Instructions:"), 0, 1);
            grid.add(noteField, 1, 1);
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    int newQty = qtySpinner.getValue();
                    int diff = newQty - (int) item.getQuantity();
                    if (diff > 0) {
                        for (int i = 0; i < diff; i++) cartService.addItem(item.getProduct());
                    } else if (diff < 0) {
                        for (int i = 0; i < Math.abs(diff); i++) cartService.decrementItem(item);
                    }
                    item.setSpecialInstructions(noteField.getText().trim());
                    cartTable.refresh();
                }
                return null;
            });
        }

        dialog.showAndWait();
    }

    // ── Split Bill ────────────────────────────────────────────────────────────
    @FXML
    private void handleSplitBill() {
        if (cartService == null || cartService.getCartItems().isEmpty()) {
            showInfo("Split Bill", "The basket is empty.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Split Bill");
        dialog.setHeaderText("Divide the bill equally");

        Spinner<Integer> peopleSpinner = new Spinner<>(2, 20, 2);
        peopleSpinner.setEditable(true);
        peopleSpinner.setPrefWidth(80);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        resultLabel.setWrapText(true);

        Button calcBtn = new Button("Calculate Split");
        calcBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        calcBtn.setMaxWidth(Double.MAX_VALUE);
        calcBtn.setOnAction(e -> {
            int people = peopleSpinner.getValue();
            double total = cartService.getTotal();
            double perPerson = total / people;
            resultLabel.setText(String.format(
                    "Total: £%.2f  ÷  %d people\n= £%.2f each", total, people, perPerson));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.add(new Label("Number of people:"), 0, 0);
        grid.add(peopleSpinner, 1, 0);
        grid.add(calcBtn, 0, 1, 2, 1);
        grid.add(resultLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ── Add Bag ───────────────────────────────────────────────────────────────
    @FXML
    private void handleAddBag() {
        if (cartService == null) return;

        for (OrderItem item : cartService.getCartItems()) {
            if ("Carrier Bag".equals(item.getProduct().getName())) {
                cartService.addItem(item.getProduct());
                return;
            }
        }

        Product bag = new Product();
        bag.setName("Carrier Bag");
        bag.setPrice(0.30);
        bag.setCategory("Miscellaneous");
        bag.setStock(999);
        cartService.addItem(bag);
    }

    // ── Clear Cart ────────────────────────────────────────────────────────────
    @FXML
    private void clearCart() {
        if (cartService != null) {
            cartService.clearCart();
        }
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
