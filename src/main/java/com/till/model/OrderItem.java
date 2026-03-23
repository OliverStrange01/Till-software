package com.till.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

public class OrderItem {

    private final Product product;
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);
    private final DoubleProperty subtotal = new SimpleDoubleProperty();

    /**
     * Constructor with explicit quantity
     */
    public OrderItem(Product product, int initialQty) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        this.product = product;
        setQuantity(initialQty);

        subtotal.bind(
                Bindings.multiply(product.priceProperty(), quantity)
        );
    }

    /**
     * Convenience constructor – default quantity = 1
     */
    public OrderItem(Product product) {
        this(product, 1);
    }

    // ────────────────────────────────────────────────
    // Quantity handling (with bounds checking)
    // ────────────────────────────────────────────────

    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int qty) {
        quantity.set(Math.max(0, qty));     // never allow negative quantity
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public void increaseQuantity() {
        increaseQuantity(1);
    }

    public void increaseQuantity(int amount) {
        if (amount > 0) {
            setQuantity(getQuantity() + amount);
        }
    }

    public void decreaseQuantity() {
        decreaseQuantity(1);
    }

    public void decreaseQuantity(int amount) {
        if (amount > 0) {
            setQuantity(getQuantity() - amount);
        }
    }

    // ────────────────────────────────────────────────
    // Subtotal (bound to price × quantity)
    // ────────────────────────────────────────────────

    public double getSubtotal() {
        return subtotal.get();
    }

    public DoubleProperty subtotalProperty() {
        return subtotal;
    }

    // ────────────────────────────────────────────────
    // Product access
    // ────────────────────────────────────────────────

    public Product getProduct() {
        return product;
    }

    @Override
    public String toString() {
        return String.format("%s × %d = £%.2f",
                product.getName(), getQuantity(), getSubtotal());
    }
}