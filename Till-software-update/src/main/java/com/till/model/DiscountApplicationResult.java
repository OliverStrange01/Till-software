package com.till.model;

public class DiscountApplicationResult {
    private final boolean success;
    private final String message;
    private final double discountAmount;
    private final double finalTotal;
    private final Coupon coupon;

    public DiscountApplicationResult(boolean success, String message, double discountAmount, double finalTotal, Coupon coupon) {
        this.success = success;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
        this.coupon = coupon;
    }

    public static DiscountApplicationResult failure(String message, double originalTotal) {
        return new DiscountApplicationResult(false, message, 0, originalTotal, null);
    }

    public static DiscountApplicationResult success(String message, double discountAmount, double finalTotal, Coupon coupon) {
        return new DiscountApplicationResult(true, message, discountAmount, finalTotal, coupon);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getFinalTotal() {
        return finalTotal;
    }

    public Coupon getCoupon() {
        return coupon;
    }
}
