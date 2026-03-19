package com.till.controller;

import com.till.model.Product;
import com.till.service.CartService;
import javafx.fxml.FXML;

public class ProductCategoryController {

    private CartService cartService;

    public void setCartService(CartService service) {
        this.cartService = service;
    }

    @FXML
    private void addCoffee() {
        cartService.addItem(new Product("coffee", "Coffee", 4.50));
    }

    @FXML
    private void addSandwich() {
        cartService.addItem(new Product("sandwich", "Sandwich", 7.00));
    }

    @FXML
    private void addMuffin() {                       // ← this one was missing or misspelled
        cartService.addItem(new Product("muffin", "Muffin", 3.20));
    }

    @FXML
    private void addWater() {
        cartService.addItem(new Product("water", "Water", 1.50));
    }
}
