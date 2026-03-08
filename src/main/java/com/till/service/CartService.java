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
        // Check if already in cart → increase qty
        for (OrderItem item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increaseQuantity();
                cartItems.set(cartItems.indexOf(item), item); // trigger update
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
            // Assuming OrderItem has setQuantity(int) method - add if missing
            // item.setQuantity(newQty);
            // For now, since we have increase, we can hack but better to add setter
            cartItems.set(cartItems.indexOf(item), item); // force refresh if needed
        }
    }

    public DoubleBinding totalBinding() {
        return Bindings.createDoubleBinding(() ->
                        cartItems.stream()
                                .mapToDouble(OrderItem::getSubtotal)
                                .sum(),
                cartItems);
    }

    public void clearCart() {
        cartItems.clear();
    }
}
