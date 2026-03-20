package com.till.controller;

import com.till.model.OrderItem;
import com.till.model.Product;
import com.till.service.CartService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    @FXML private Button payButton;
    @FXML private Button clearButton;

    private CartService cartService;

    public void setCartService(CartService service) {
        this.cartService = service;

        // Bind the table and total label now that we have the service
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
        // Name column – use observable property from Product
        nameCol.setCellValueFactory(cellData ->
                cellData.getValue().getProduct().nameProperty()
        );

        // Quantity column – uses IntegerProperty
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        // Price column – use observable property
        priceCol.setCellValueFactory(cellData ->
                cellData.getValue().getProduct().priceProperty().asObject()
        );

        // Subtotal column – dynamic, depends on quantity
        subtotalCol.setCellValueFactory(cellData ->
                Bindings.createDoubleBinding(
                        cellData.getValue()::getSubtotal,
                        cellData.getValue().quantityProperty()
                ).asObject()
        );

        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        subtotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<OrderItem, Void> removeCol = new TableColumn<>("Remove");
        removeCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<OrderItem, Void> call(final TableColumn<OrderItem, Void> param) {
                final TableCell<OrderItem, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("X");

                    {
                        btn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                        btn.setOnAction(event -> {
                            OrderItem item = getTableView().getItems().get(getIndex());
                            cartService.removeItem(item);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btn);
                    }
                };
                return cell;
            }
        });
        removeCol.setPrefWidth(70);
        removeCol.setMaxWidth(70);
        removeCol.setMinWidth(70);
        cartTable.getColumns().add(removeCol);
    }

    @FXML
    private void clearCart() {
        if (cartService != null) {
            cartService.clearCart();
        }
    }

    @FXML
    private void payNow() {
        if (cartService == null) {
            new Alert(Alert.AlertType.WARNING, "Cart service not initialized").showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment");
        alert.setHeaderText("Total: " + totalLabel.getText());
        alert.setContentText("Payment simulation – Accepted!\n(Implement real payment later)");
        alert.showAndWait();
    }
}