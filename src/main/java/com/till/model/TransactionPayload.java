package com.till.model;

import java.util.List;

public class TransactionPayload {
    private final List<OrderItem> items;
    private final double subTotal;
    private final double discountAmount;
    private final double total;
    private final String paymentMethod;
    private final String couponCode;

    public TransactionPayload(List<OrderItem> items, double subTotal, double discountAmount, double total, String paymentMethod, String couponCode) {
        this.items = items;
        this.subTotal = subTotal;
        this.discountAmount = discountAmount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTotal() {
        return total;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }
}
