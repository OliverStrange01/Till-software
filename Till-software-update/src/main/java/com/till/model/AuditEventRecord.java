package com.till.model;

public class AuditEventRecord {
    private final long id;
    private final String eventType;
    private final String details;
    private final String actor;
    private final String createdAt;

    public AuditEventRecord(long id, String eventType, String details, String actor, String createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.details = details;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetails() {
        return details;
    }

    public String getActor() {
        return actor;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
