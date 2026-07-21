package com.controlleads.notifications;

import com.controlleads.activities.Activity;
import com.controlleads.activities.ActivityRepository;
import com.controlleads.common.CurrentUser;
import com.controlleads.leads.Lead;
import com.controlleads.leads.LeadRepository;
import com.controlleads.settings.AppSettingsService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    // A follow-up reminder repeats at most once per day while it stays overdue.
    private static final Duration FOLLOW_UP_DEDUP = Duration.ofHours(24);

    private final NotificationRepository notifications;
    private final LeadRepository leads;
    private final ActivityRepository activities;
    private final AppSettingsService settings;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository notifications, LeadRepository leads,
                               ActivityRepository activities, AppSettingsService settings,
                               ApplicationEventPublisher events) {
        this.notifications = notifications;
        this.leads = leads;
        this.activities = activities;
        this.settings = settings;
        this.events = events;
    }

    // ---- detection sweeps (called by the scheduler, or directly in tests) ----

    /** Alert owners of HOT_LEADs that went cold; at most one alert per SLA cycle. */
    @Transactional
    public int runSlaSweep() {
        int hours = settings.hotLeadMaxHours();
        Instant threshold = Instant.now().minus(Duration.ofHours(hours));
        Duration dedup = Duration.ofHours(hours);
        int created = 0;
        for (Lead lead : leads.findSlaBreaching(threshold)) {
            if (createIfAbsent(lead.getAssignedTo(), lead.getId(), NotificationType.SLA_BREACH, dedup)) {
                created++;
            }
        }
        return created;
    }

    /** Remind owners of follow-ups whose due date has passed. */
    @Transactional
    public int runFollowUpSweep() {
        int created = 0;
        for (Activity followUp : activities.findDueFollowUps(Instant.now())) {
            Lead lead = leads.findById(followUp.getLeadId()).orElse(null);
            if (lead == null || lead.getDeletedAt() != null) {
                continue;
            }
            if (createIfAbsent(lead.getAssignedTo(), lead.getId(), NotificationType.FOLLOW_UP_DUE, FOLLOW_UP_DEDUP)) {
                created++;
            }
        }
        return created;
    }

    /** Create a notification unless an equivalent one already fired within the window. */
    private boolean createIfAbsent(UUID userId, UUID leadId, NotificationType type, Duration dedup) {
        Instant since = Instant.now().minus(dedup);
        if (notifications.existsByUserIdAndLeadIdAndTypeAndCreatedAtAfter(userId, leadId, type, since)) {
            return false;
        }
        Notification saved = notifications.save(new Notification(userId, leadId, type));
        // Dispatched only after this transaction commits (see NotificationDispatchListener).
        events.publishEvent(new NotificationCreatedEvent(saved));
        return true;
    }

    // ---- in-app center -------------------------------------------------------

    public List<Notification> recent(CurrentUser caller) {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(caller.id());
    }

    public long unreadCount(CurrentUser caller) {
        return notifications.countByUserIdAndReadAtIsNull(caller.id());
    }

    @Transactional
    public void markRead(UUID id, CurrentUser caller) {
        Optional<Notification> found = notifications.findByIdAndUserId(id, caller.id());
        found.ifPresent(Notification::markRead);
    }

    @Transactional
    public void markAllRead(CurrentUser caller) {
        notifications.markAllRead(caller.id(), Instant.now());
    }
}
