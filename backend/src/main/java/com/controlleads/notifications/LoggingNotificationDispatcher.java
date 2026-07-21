package com.controlleads.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default dispatcher — logs instead of sending. Swap in FCM/APNs (push) and an
 * email digest sender by providing your own NotificationDispatcher bean
 * (see NotificationConfig, which backs off when one is present).
 */
public class LoggingNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationDispatcher.class);

    @Override
    public void dispatch(Notification notification) {
        log.info("notification {} type={} user={} lead={} (in-app only; wire push/email here)",
            notification.getId(), notification.getType(), notification.getUserId(),
            notification.getLeadId());
    }
}
