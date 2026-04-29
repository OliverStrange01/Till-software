package com.till.model;

import java.time.LocalDateTime;

public class AssistanceCall {
    private final AssistanceType type;
    private final String reason;
    private final LocalDateTime createdAt;

    public AssistanceCall(AssistanceType type, String reason) {
        this.type = type;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public AssistanceType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
