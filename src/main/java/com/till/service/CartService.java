package com.till.service;

import com.till.model.OrderItem;
import com.till.model.Product;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class CartService {

    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();

    public ObservableList<OrderItem> getCartItems() {
        return cartItems;
    }

    public void addItem(Product product) {
        for (OrderItem item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increaseQuantity();
                return;
            }
        }
        cartItems.add(new OrderItem(product));   // ← uses 1-arg constructor
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
        List<Observable> dependencies = new ArrayList<>();
        dependencies.add(cartItems);  // list changes (add/remove)

        // Add every item's subtotal as a dependency
        for (OrderItem item : cartItems) {
            dependencies.add(item.subtotalProperty());
        }

        return Bindings.createDoubleBinding(
                () -> cartItems.stream().mapToDouble(OrderItem::getSubtotal).sum(),
                dependencies.toArray(new Observable[0])
        );
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
