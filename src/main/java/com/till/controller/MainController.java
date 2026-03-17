package com.till.controller;

import com.till.model.Products;
import com.till.service.CartService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MainController {

    private final CartService cartService = new CartService();

    @FXML
    private Label totalLabel;

    @FXML
    private TextField cashField;

    @FXML
    private Label resultLabel;

    @FXML
    public void initialize() {
        // examples
        cartService.addProduct(new Products("1", "Milk", 1.50));
        cartService.addProduct(new Products("2", "Bread", 1.20));
        cartService.addProduct(new Products("1", "Milk", 1.50));

        updateTotal();
    }

    @FXML
    private void handleCheckout() {
        try {
            double cashGiven = Double.parseDouble(cashField.getText());
            double total = cartService.getTotal();
            double change = cartService.calculateChange(cashGiven);

            if (change < 0) {
                resultLabel.setText("Not enough cash. Total is £" + String.format("%.2f", total));

            } else {
                resultLabel.setText("Change: £" + String.format("%.2f", change));
                cartService.clearCart();
                updateTotal();
                cashField.clear();
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Please enter a valid amount of cash.");
        }
    }

    private void updateTotal() {
        totalLabel.setText("Total: £" + String.format("%.2f", cartService.getTotal()));
    }
}
