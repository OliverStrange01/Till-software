package com.till.model;

public class Coupon {
    private final String code;
    private final DiscountRule rule;

    public Coupon(String code, DiscountRule rule) {
        this.code = code;
        this.rule = rule;
    }

    public String getCode() {
        return code;
    }

    public DiscountRule getRule() {
        return rule;
    }
}
