package com.controlleads.leads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Immutable record of an ownership change — conversion credit audit trail. */
@Entity
@Table(name = "lead_assignment_events")
public class LeadAssignmentEvent {

    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "from_user")
    private UUID fromUser;

    @Column(name = "to_user", nullable = false)
    private UUID toUser;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected LeadAssignmentEvent() {
    }

    public LeadAssignmentEvent(UUID leadId, UUID fromUser, UUID toUser, UUID changedBy) {
        this.id = UUID.randomUUID();
        this.leadId = leadId;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public UUID getLeadId() { return leadId; }
    public UUID getFromUser() { return fromUser; }
    public UUID getToUser() { return toUser; }
    public Instant getChangedAt() { return changedAt; }
}
