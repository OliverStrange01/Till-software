package com.till.service;

import com.till.model.OrderItem;
import com.till.model.Product;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import java.util.Objects;

public class CartService {

    // The extractor tells JavaFX to also watch quantityProperty on each item.
    // Without this, the list only fires events for add/remove — not quantity changes.
    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList(
            item -> new Observable[]{ item.quantityProperty(), item.subtotalProperty() }
    );

    private final DoubleProperty total = new SimpleDoubleProperty(0);

    public CartService() {
        cartItems.addListener((ListChangeListener<OrderItem>) change -> recalcTotal());
    }

    private void recalcTotal() {
        total.set(
                cartItems.stream()
                        .mapToDouble(OrderItem::getSubtotal)
                        .sum()
        );
    }

    public ObservableList<OrderItem> getCartItems() {
        return cartItems;
    }

    // Return type changed to DoubleProperty — CartController.totalBinding() call stays the same
    public DoubleProperty totalBinding() {
        return total;
    }

    public double getTotal() {
        return total.get();
    }

    public void addItem(Product product) {
        addItem(product, 1);
    }

    public void addItem(Product product, double quantity) {
        Objects.requireNonNull(product, "Product cannot be null");

        for (OrderItem item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increaseQuantity(quantity);
                return;
            }
        }

        cartItems.add(new OrderItem(product, quantity));
    }

    public void removeItem(OrderItem item) {
        cartItems.remove(item);
    }

    public void decrementItem(OrderItem item) {
        if (item.getQuantity() <= 1) {
            removeItem(item);
        } else {
            item.setQuantity(item.getQuantity() - 1);
        }
    }

    public void updateQuantity(OrderItem item, double newQty) {
        if (newQty <= 0) {
            removeItem(item);
        } else {
            item.setQuantity(newQty);
        }
    }

    public void clearCart() {
        cartItems.clear();
    }
}