package com.till.service;

import com.till.model.Coupon;
import com.till.model.DiscountApplicationResult;
import com.till.model.DiscountRule;
import com.till.model.DiscountType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DiscountServiceTest {

    @Test
    void appliesPercentageCoupon() {
        DiscountService service = new DiscountService();
        service.registerCoupon(new Coupon("TEST10",
                new DiscountRule(DiscountType.PERCENTAGE, 10, LocalDate.now().plusDays(1), 0, false)));

        DiscountApplicationResult result = service.applyCoupon("test10", 50, null);

        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getDiscountAmount(), 0.001);
        assertEquals(45.0, result.getFinalTotal(), 0.001);
    }

    @Test
    void rejectsExpiredCoupon() {
        DiscountService service = new DiscountService();
        service.registerCoupon(new Coupon("OLD5",
                new DiscountRule(DiscountType.FIXED, 5, LocalDate.now().minusDays(1), 0, false)));

        DiscountApplicationResult result = service.applyCoupon("OLD5", 50, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("expired"));
    }

    @Test
    void rejectsNonStackableCouponWhenExistingCouponPresent() {
        DiscountService service = new DiscountService();
        Coupon first = new Coupon("FIRST5",
                new DiscountRule(DiscountType.FIXED, 5, LocalDate.now().plusDays(5), 0, false));
        Coupon second = new Coupon("SECOND5",
                new DiscountRule(DiscountType.FIXED, 5, LocalDate.now().plusDays(5), 0, false));
        service.registerCoupon(first);
        service.registerCoupon(second);

        DiscountApplicationResult result = service.applyCoupon("SECOND5", 40, first);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("stacked"));
    }
}
