package com.controlleads.activities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(nullable = false)
    private String content;

    @Column(name = "due_at")
    private Instant dueAt;          // FOLLOW_UP only

    @Column(name = "completed_at")
    private Instant completedAt;    // FOLLOW_UP only

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Activity() {
    }

    public Activity(UUID leadId, ActivityType type, String content, Instant dueAt, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.leadId = leadId;
        this.type = type;
        this.content = content;
        this.dueAt = dueAt;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void complete() {
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getLeadId() { return leadId; }
    public ActivityType getType() { return type; }
    public String getContent() { return content; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCompletedAt() { return completedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
