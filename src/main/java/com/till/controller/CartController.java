package com.till.controller;

import com.till.model.OrderItem;
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

        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        priceCol.setCellValueFactory(cellData ->
                cellData.getValue().getProduct().priceProperty().asObject()
        );

        subtotalCol.setCellValueFactory(cellData ->
                cellData.getValue().subtotalProperty().asObject()
        );

        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        subtotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Remove column
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
        cartTable.getColumns().add(removeCol);
    }

    @FXML
    private void clearCart() {
        if (cartService != null) {
            cartService.clearCart();
        }
    }
}