package com.till.model;

import java.time.LocalDate;

public class DiscountRule {
    private final DiscountType type;
    private final double value;
    private final LocalDate expiryDate;
    private final double minimumSpend;
    private final boolean stackable;

    public DiscountRule(DiscountType type, double value, LocalDate expiryDate, double minimumSpend, boolean stackable) {
        this.type = type;
        this.value = value;
        this.expiryDate = expiryDate;
        this.minimumSpend = minimumSpend;
        this.stackable = stackable;
    }

    public DiscountType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public double getMinimumSpend() {
        return minimumSpend;
    }

    public boolean isStackable() {
        return stackable;
    }
}
