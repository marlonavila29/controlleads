package com.controlleads.notifications;

/** Published when a notification is persisted; dispatched after commit. */
public record NotificationCreatedEvent(Notification notification) {
}
