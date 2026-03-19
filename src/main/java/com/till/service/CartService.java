package com.till.service;

import com.till.model.OrderItem;
import com.till.model.Product;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartService {

    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();

    public ObservableList<OrderItem> getCartItems() {
        return cartItems;
    }

    public void addItem(Product product) {
        // Check if already in cart → increase quantity
        for (OrderItem item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increaseQuantity();
                // Trigger UI update (ObservableList fires change event automatically)
                return;
            }
        }
        // New item
        cartItems.add(new OrderItem(product));
    }

    public void removeItem(OrderItem item) {
        cartItems.remove(item);
    }

    public void updateQuantity(OrderItem item, int newQty) {
        if (newQty <= 0) {
            removeItem(item);
        } else {
            item.setQuantity(newQty);  // make sure this method exists in OrderItem
        }
    }

    public DoubleBinding totalBinding() {
        return Bindings.createDoubleBinding(() ->
                cartItems.stream()
                        .mapToDouble(OrderItem::getSubtotal)
                        .sum(),
                cartItems);
    }

    public double getTotal() {
        return cartItems.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }

    public double calculateChange(double cashGiven) {
        double total = getTotal();
        if (cashGiven < total) {
            return -1; // Not enough cash
        }
        return cashGiven - total;
    }

    public void clearCart() {
        cartItems.clear();
    }
}
