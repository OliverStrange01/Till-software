package com.till.service;

import com.till.model.AssistanceCall;
import com.till.model.AssistanceType;
import com.till.model.OrderItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssistanceService {
    private static final double SECURITY_THRESHOLD = 250.0;
    private static final int HIGH_QUANTITY_THRESHOLD = 20;

    public List<AssistanceCall> evaluateAssistanceNeeds(List<OrderItem> items, double finalTotal, boolean discountApplied) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<AssistanceCall> calls = new ArrayList<>();

        if (discountApplied && finalTotal >= 100) {
            calls.add(new AssistanceCall(
                    AssistanceType.MANAGER_OVERRIDE,
                    "Discounted basket above override threshold."
            ));
        }

        if (finalTotal >= SECURITY_THRESHOLD) {
            calls.add(new AssistanceCall(
                    AssistanceType.SECURITY_ALERT,
                    "High-value transaction flagged for verification."
            ));
        }

        for (OrderItem item : items) {
            if (item.getProduct().getPrice() <= 0) {
                calls.add(new AssistanceCall(
                        AssistanceType.PRICE_CHECK,
                        "Price check needed for " + item.getProduct().getName() + "."
                ));
            }
            if (item.getQuantity() >= HIGH_QUANTITY_THRESHOLD) {
                calls.add(new AssistanceCall(
                        AssistanceType.SECURITY_ALERT,
                        "Large quantity purchased for " + item.getProduct().getName() + "."
                ));
            }
        }

        return calls;
    }
}
