package com.till.controller;

import java.util.ArrayList;
import java.util.List;

import com.till.model.Products;
import com.till.service.CartService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MainController {

    private final CartService cartService = new CartService();
    private final List<Products> products = new ArrayList<>();

    @FXML
    private Label totalLabel;

    @FXML
    private TextField codeField;

    @FXML
    private TextField cashField;

    @FXML
    private Label resultLabel;

    @FXML
public void initialize() {
    Products milk = new Products("1", "1001", "Milk", 1.50);
    Products bread = new Products("2", "1002", "Bread", 1.20);
    Products eggs = new Products("3", "1003", "Eggs", 2.00);

    products.add(milk);
    products.add(bread);
    products.add(eggs);

    // examples
    cartService.addProduct(milk);
    cartService.addProduct(bread);
    cartService.addProduct(milk);

    updateTotal();
}

    @FXML
    private void handleCashPayment() {
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

    @FXML
    private void handleAddByCode() {
        String code = codeField.getText();

        if (code == null || code.isBlank()) {
            resultLabel.setText("Enter a product code.");
            return;
        }

        Products foundProduct = cartService.findProductByCode(products, code);

        if (foundProduct == null) {
            resultLabel.setText("Product code not found.");
            return;
        }

        cartService.addProduct(foundProduct);
        updateTotal();
        resultLabel.setText(foundProduct.getName() + " added.");
        codeField.clear();
    }

    private void updateTotal() {
        totalLabel.setText("Total: £" + String.format("%.2f", cartService.getTotal()));
    }
}
