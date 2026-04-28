package com.till.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

public class OrderItem {

    private final Product product;
    private final DoubleProperty quantity = new SimpleDoubleProperty(1);
    private final DoubleProperty subtotal = new SimpleDoubleProperty();

    /**
     * Constructor with explicit quantity
     */
    public OrderItem(Product product, double initialQty) {
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

    public double getQuantity() {
        return quantity.get();
    }

    public void setQuantity(double qty) {
        quantity.set(Math.max(0, qty));
    }

    public DoubleProperty quantityProperty() {
        return quantity;
    }

    public void increaseQuantity() {
        increaseQuantity(1);
    }

    public void increaseQuantity(double amount) {
        if (amount > 0) {
            setQuantity(getQuantity() + amount);
        }
    }

    public DoubleProperty subtotalProperty() {
        return subtotal;
    }

    public void decreaseQuantity() {
        decreaseQuantity(1);
    }

    public void decreaseQuantity(double amount) {
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

    // ────────────────────────────────────────────────
    // Product access
    // ────────────────────────────────────────────────

    public Product getProduct() {
        return product;
    }

    @Override
    public String toString() {
        return String.format("%s × %.2f %s = £%.2f",
                product.getName(), getQuantity(), product.getUnit(), getSubtotal());
    }
}