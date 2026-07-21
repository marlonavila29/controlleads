package com.controlleads.leads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of one funnel transition — THE source of every
 * conversion/drop-off report (CLAUDE.md rule 2). Never updated or deleted.
 */
@Entity
@Table(name = "lead_status_events")
public class LeadStatusEvent {

    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private LeadStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private LeadStatus toStatus;

    @Column(name = "stall_reason_id")
    private UUID stallReasonId;

    private String note;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected LeadStatusEvent() {
    }

    public LeadStatusEvent(UUID leadId, LeadStatus fromStatus, LeadStatus toStatus,
                           UUID stallReasonId, String note, UUID changedBy) {
        this.id = UUID.randomUUID();
        this.leadId = leadId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.stallReasonId = stallReasonId;
        this.note = note;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getLeadId() { return leadId; }
    public LeadStatus getFromStatus() { return fromStatus; }
    public LeadStatus getToStatus() { return toStatus; }
    public UUID getStallReasonId() { return stallReasonId; }
    public String getNote() { return note; }
    public UUID getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
