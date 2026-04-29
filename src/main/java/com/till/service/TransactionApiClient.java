package com.till.service;

import com.till.model.OrderItem;
import com.till.model.TransactionPayload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionApiClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final HttpSender sender;
    private final String endpoint;
    private final String apiKey;

    public TransactionApiClient(String endpoint, String apiKey) {
        this(endpoint, apiKey, new JavaHttpSender());
    }

    public TransactionApiClient(String endpoint, String apiKey, HttpSender sender) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.sender = Objects.requireNonNull(sender, "HTTP sender cannot be null");
    }

    public boolean submit(TransactionPayload payload) {
        if (endpoint == null || endpoint.isBlank()) {
            return false;
        }

        String body = serialisePayload(payload);
        return submitJson(body);
    }

    public boolean submitJson(String payloadJson) {
        if (endpoint == null || endpoint.isBlank()) {
            return false;
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (apiKey == null ? "" : apiKey))
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                .build();

        try {
            int status = sender.send(request);
            return status >= 200 && status < 300;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public String serialisePayload(TransactionPayload payload) {
        String items = payload.getItems().stream()
                .map(this::itemToJson)
                .collect(Collectors.joining(","));

        return "{"
                + "\"subTotal\":" + round(payload.getSubTotal()) + ","
                + "\"discountAmount\":" + round(payload.getDiscountAmount()) + ","
                + "\"total\":" + round(payload.getTotal()) + ","
                + "\"paymentMethod\":\"" + escape(payload.getPaymentMethod()) + "\","
                + "\"couponCode\":\"" + escape(payload.getCouponCode() == null ? "" : payload.getCouponCode()) + "\","
                + "\"items\":[" + items + "]"
                + "}";
    }

    private String itemToJson(OrderItem item) {
        return "{"
                + "\"productId\":\"" + escape(item.getProduct().getId()) + "\","
                + "\"name\":\"" + escape(item.getProduct().getName()) + "\","
                + "\"quantity\":" + item.getQuantity() + ","
                + "\"price\":" + round(item.getProduct().getPrice()) + ","
                + "\"subtotal\":" + round(item.getSubtotal())
                + "}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String round(double value) {
        return String.format(Locale.UK, "%.2f", value);
    }

    public interface HttpSender {
        int send(HttpRequest request) throws IOException, InterruptedException;
    }

    public static class JavaHttpSender implements HttpSender {
        private final HttpClient client = HttpClient.newHttpClient();

        @Override
        public int send(HttpRequest request) throws IOException, InterruptedException {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        }
    }
}
