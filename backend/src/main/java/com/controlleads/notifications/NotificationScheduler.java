package com.controlleads.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically runs the SLA and follow-up sweeps. Disabled in tests via
 * app.scheduling.enabled=false so notification creation stays deterministic.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notifications;

    public NotificationScheduler(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "${app.notifications.sweep-ms:300000}",
               initialDelayString = "${app.notifications.sweep-ms:300000}")
    public void sweep() {
        try {
            int sla = notifications.runSlaSweep();
            int followUps = notifications.runFollowUpSweep();
            if (sla + followUps > 0) {
                log.info("notification sweep created {} SLA + {} follow-up alerts", sla, followUps);
            }
        } catch (RuntimeException e) {
            log.error("notification sweep failed", e);
        }
    }
}
