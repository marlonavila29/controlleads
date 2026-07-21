package com.controlleads.auth;

/** Published when a reset token is persisted; the email goes out after commit. */
public record PasswordResetRequestedEvent(String email, String rawToken) {
}
