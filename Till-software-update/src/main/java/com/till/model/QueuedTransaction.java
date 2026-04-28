package com.till.model;

public class QueuedTransaction {
    private final long id;
    private final String payloadJson;
    private final int attempts;
    private final String status;
    private final String lastError;
    private final String createdAt;
    private final String nextAttemptAt;

    public QueuedTransaction(long id, String payloadJson, int attempts, String status, String lastError, String createdAt, String nextAttemptAt) {
        this.id = id;
        this.payloadJson = payloadJson;
        this.attempts = attempts;
        this.status = status;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.nextAttemptAt = nextAttemptAt;
    }

    public long getId() {
        return id;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getStatus() {
        return status;
    }

    public String getLastError() {
        return lastError;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getNextAttemptAt() {
        return nextAttemptAt;
    }
}
