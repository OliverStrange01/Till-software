package com.till.service;

import com.till.model.Coupon;
import com.till.model.DiscountApplicationResult;
import com.till.model.DiscountRule;
import com.till.model.DiscountType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DiscountService {
    private final Map<String, Coupon> coupons = new HashMap<>();

    public DiscountService() {
        // Sample coupons for local development and demos.
        registerCoupon(new Coupon("WELCOME10",
                new DiscountRule(DiscountType.PERCENTAGE, 10, LocalDate.now().plusYears(1), 0, false)));
        registerCoupon(new Coupon("SAVE5",
                new DiscountRule(DiscountType.FIXED, 5, LocalDate.now().plusYears(1), 20, false)));
    }

    public void registerCoupon(Coupon coupon) {
        Objects.requireNonNull(coupon, "Coupon cannot be null");
        Objects.requireNonNull(coupon.getRule(), "Coupon rule cannot be null");
        coupons.put(normaliseCode(coupon.getCode()), coupon);
    }

    public DiscountApplicationResult applyCoupon(String rawCode, double subTotal, Coupon alreadyApplied) {
        if (rawCode == null || rawCode.isBlank()) {
            return DiscountApplicationResult.failure("Please enter a coupon code.", subTotal);
        }

        Coupon coupon = coupons.get(normaliseCode(rawCode));
        if (coupon == null) {
            return DiscountApplicationResult.failure("Coupon code is not recognised.", subTotal);
        }

        DiscountRule rule = coupon.getRule();
        if (rule.getExpiryDate() != null && rule.getExpiryDate().isBefore(LocalDate.now())) {
            return DiscountApplicationResult.failure("Coupon has expired.", subTotal);
        }
        if (subTotal < rule.getMinimumSpend()) {
            return DiscountApplicationResult.failure(
                    String.format("Coupon requires a minimum spend of £%.2f.", rule.getMinimumSpend()),
                    subTotal
            );
        }
        if (alreadyApplied != null && !rule.isStackable()) {
            // Default stance is non-stacking unless a rule explicitly allows it.
            return DiscountApplicationResult.failure("This coupon cannot be stacked with another discount.", subTotal);
        }

        double discount = switch (rule.getType()) {
            case PERCENTAGE -> subTotal * (rule.getValue() / 100.0);
            case FIXED -> rule.getValue();
        };
        double finalTotal = Math.max(0, subTotal - discount);
        double clampedDiscount = subTotal - finalTotal;

        return DiscountApplicationResult.success(
                String.format(Locale.UK, "Applied %s: -£%.2f", coupon.getCode(), clampedDiscount),
                clampedDiscount,
                finalTotal,
                coupon
        );
    }

    private String normaliseCode(String code) {
        return code.trim().toUpperCase(Locale.UK);
    }
}
