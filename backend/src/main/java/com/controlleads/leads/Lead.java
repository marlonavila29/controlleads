package com.controlleads.leads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    private String email;

    private String phone;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "utm_source")
    private String utmSource;

    @Column(name = "utm_medium")
    private String utmMedium;

    @Column(name = "utm_campaign")
    private String utmCampaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.LEAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "stalled_from_status")
    private LeadStatus stalledFromStatus;

    @Column(name = "stall_reason_id")
    private UUID stallReasonId;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Lead() {
    }

    public Lead(String fullName, String countryCode, String email, String phone,
                UUID courseId, UUID channelId, UUID assignedTo, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.countryCode = countryCode;
        this.email = email;
        this.phone = phone;
        this.courseId = courseId;
        this.channelId = channelId;
        this.assignedTo = assignedTo;
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

    /** Applies a validated transition; STALLED bookkeeping happens here. */
    void moveTo(LeadStatus to, UUID stallReasonId) {
        if (to == LeadStatus.STALLED) {
            this.stalledFromStatus = this.status;
            this.stallReasonId = stallReasonId;
        } else {
            this.stalledFromStatus = null;
            this.stallReasonId = null;
        }
        this.status = to;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getCountryCode() { return countryCode; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UUID getCourseId() { return courseId; }
    public UUID getChannelId() { return channelId; }
    public String getUtmSource() { return utmSource; }
    public String getUtmMedium() { return utmMedium; }
    public String getUtmCampaign() { return utmCampaign; }
    public LeadStatus getStatus() { return status; }
    public LeadStatus getStalledFromStatus() { return stalledFromStatus; }
    public UUID getStallReasonId() { return stallReasonId; }
    public UUID getAssignedTo() { return assignedTo; }
    public Instant getLastContactedAt() { return lastContactedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public void setChannelId(UUID channelId) { this.channelId = channelId; }
    public void setUtmSource(String utmSource) { this.utmSource = utmSource; }
    public void setUtmMedium(String utmMedium) { this.utmMedium = utmMedium; }
    public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
    public void setLastContactedAt(Instant lastContactedAt) { this.lastContactedAt = lastContactedAt; }
}
