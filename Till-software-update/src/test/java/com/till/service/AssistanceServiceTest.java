package com.till.service;

import com.till.model.AssistanceCall;
import com.till.model.AssistanceType;
import com.till.model.OrderItem;
import com.till.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistanceServiceTest {

    @Test
    void createsManagerOverrideForLargeDiscountedBasket() {
        AssistanceService service = new AssistanceService();
        OrderItem item = new OrderItem(new Product("P1", "Widget", 120.0), 1);

        List<AssistanceCall> calls = service.evaluateAssistanceNeeds(List.of(item), 120.0, true);

        assertTrue(calls.stream().anyMatch(c -> c.getType() == AssistanceType.MANAGER_OVERRIDE));
    }

    @Test
    void createsPriceCheckWhenItemHasZeroPrice() {
        AssistanceService service = new AssistanceService();
        OrderItem item = new OrderItem(new Product("P2", "No Price", 0.0), 1);

        List<AssistanceCall> calls = service.evaluateAssistanceNeeds(List.of(item), 0.0, false);

        assertTrue(calls.stream().anyMatch(c -> c.getType() == AssistanceType.PRICE_CHECK));
    }
}
