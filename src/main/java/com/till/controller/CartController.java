package com.till.controller;

import com.till.model.OrderItem;
import com.till.service.CartService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML private TableView<OrderItem> cartTable;
    @FXML private TableColumn<OrderItem, String> nameCol;
    @FXML private TableColumn<OrderItem, Integer> qtyCol;
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

        // Wrap quantity in an observable so the cell re-renders on change
        qtyCol.setCellValueFactory(cellData ->
                cellData.getValue().quantityProperty().asObject()
        );

        priceCol.setCellValueFactory(cellData ->
                cellData.getValue().getProduct().priceProperty().asObject()
        );

        // subtotalProperty() must be a DoubleBinding tied to quantity * price
        subtotalCol.setCellValueFactory(cellData ->
                cellData.getValue().subtotalProperty().asObject()
        );

        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        subtotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Format price/subtotal columns to show £ and 2 d.p.
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("£%.2f", value));
            }
        });

        subtotalCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("£%.2f", value));
            }
        });

        // Actions column: [ - ] [ X ]
        TableColumn<OrderItem, Void> actionsCol = new TableColumn<>("Qty");
        actionsCol.setPrefWidth(90);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button decrementBtn = new Button("-");
            private final Button removeBtn = new Button("X");
            private final HBox box = new HBox(4, decrementBtn, removeBtn);

            {
                decrementBtn.setStyle("-fx-background-color: #f0a500; -fx-text-fill: white; -fx-font-weight: bold;");
                removeBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");

                decrementBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    cartService.decrementItem(item); // removes row if qty hits 0
                });

                removeBtn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    cartService.removeItem(item);
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

    @FXML
    private void clearCart() {
        if (cartService != null) {
            cartService.clearCart();
        }
    }
}