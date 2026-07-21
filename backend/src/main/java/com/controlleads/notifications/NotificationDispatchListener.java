package com.controlleads.notifications;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers out-of-app notifications only AFTER the creating transaction commits,
 * so a rolled-back sweep never leaves a phantom push/email. fallbackExecution
 * keeps it working if a notification is ever created outside a transaction.
 */
@Component
public class NotificationDispatchListener {

    private final NotificationDispatcher dispatcher;

    public NotificationDispatchListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        dispatcher.dispatch(event.notification());
    }
}
