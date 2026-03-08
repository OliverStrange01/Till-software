package com.till.controller;

import com.till.service.CartService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.scene.control.SplitPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private SplitPane splitPane;

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
            splitPane.setDividerPositions(0.55);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}