package com.controlleads.catalogs;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

/**
 * Base for admin-managed catalogs (courses, channels, stall reasons).
 * Items are deactivated, never deleted — history references them.
 */
@MappedSuperclass
public abstract class CatalogItem {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogItem() {
    }

    protected CatalogItem(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
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

    public UUID getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setActive(boolean active) { this.active = active; }
}
