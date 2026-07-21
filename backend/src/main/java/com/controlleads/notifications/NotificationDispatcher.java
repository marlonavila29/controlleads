package com.controlleads.notifications;

/**
 * Delivery seam for out-of-app channels (push, email). The in-app center is
 * always populated by NotificationService; this pushes the same event outward.
 * Replace the default logging implementation with real FCM/APNs + SMTP wiring
 * (needs credentials/config) without touching the rest of the module.
 */
public interface NotificationDispatcher {

    void dispatch(Notification notification);
}
