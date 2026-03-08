package com.till.model;

public class OrderItems {
    private Products product;
    private int quantity = 1;

    public OrderItems(Products product) {
        this.product = product;
    }

    public Products getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void increaseQuantity() { quantity++; }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }
}