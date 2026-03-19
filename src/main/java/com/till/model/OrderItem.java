package com.till.model;

import javafx.beans.property.*;

public class OrderItem {
    private final Product product;
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);

    public OrderItem(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int qty) {
        if (qty >= 0) {
            quantity.set(qty);
        }
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public double getSubtotal() {
        return product.getPrice() * getQuantity();
    }

    public void increaseQuantity() {
        quantity.set(quantity.get() + 1);
    }

    // Optional: decrease
    public void decreaseQuantity() {
        if (quantity.get() > 1) {
            quantity.set(quantity.get() - 1);
        } else {
            // optionally remove item from cart in service
        }
    }
}
