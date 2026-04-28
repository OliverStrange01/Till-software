package com.till.model;

import java.time.LocalDateTime;
import java.util.List;

public class ReceiptData {
    private final List<OrderItem> items;
    private final double subTotal;
    private final double discountAmount;
    private final double total;
    private final double tendered;
    private final double change;
    private final String paymentMethod;
    private final String couponCode;
    private final LocalDateTime createdAt;

    public ReceiptData(List<OrderItem> items, double subTotal, double discountAmount, double total,
                       double tendered, double change, String paymentMethod, String couponCode, LocalDateTime createdAt) {
        this.items = items;
        this.subTotal = subTotal;
        this.discountAmount = discountAmount;
        this.total = total;
        this.tendered = tendered;
        this.change = change;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
        this.createdAt = createdAt;
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

    public double getTendered() {
        return tendered;
    }

    public double getChange() {
        return change;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
