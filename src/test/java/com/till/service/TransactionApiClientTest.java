package com.till.service;

import com.till.model.OrderItem;
import com.till.model.Product;
import com.till.model.TransactionPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionApiClientTest {

    @Test
    void returnsTrueWhenApiReturnsSuccessCode() {
        TransactionApiClient client = new TransactionApiClient(
                "https://example.invalid/tx",
                "abc",
                request -> 201
        );

        boolean ok = client.submit(samplePayload());

        assertTrue(ok);
    }

    @Test
    void returnsFalseWhenApiReturnsServerError() {
        TransactionApiClient client = new TransactionApiClient(
                "https://example.invalid/tx",
                "abc",
                request -> 500
        );

        boolean ok = client.submit(samplePayload());

        assertFalse(ok);
    }

    private TransactionPayload samplePayload() {
        OrderItem item = new OrderItem(new Product("P1", "Test Item", 2.5), 2);
        return new TransactionPayload(List.of(item), 5, 0, 5, "CARD", null);
    }
}
