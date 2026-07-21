package com.controlleads.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the reset email AFTER commit and ASYNCHRONOUSLY, swallowing failures.
 * This keeps forgot-password free of a user-enumeration oracle: the response
 * returns identically (204) and at the same speed whether or not the email
 * exists, and a failing mailer can never turn the existing-email path into a 500.
 */
@Component
public class PasswordResetDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetDispatchListener.class);

    private final PasswordResetMailer mailer;

    public PasswordResetDispatchListener(PasswordResetMailer mailer) {
        this.mailer = mailer;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            mailer.sendResetLink(event.email(), event.rawToken());
        } catch (RuntimeException e) {
            // Never surface to the caller — the endpoint must stay a silent 204.
            log.error("failed to send password reset email to {}", event.email(), e);
        }
    }
}
