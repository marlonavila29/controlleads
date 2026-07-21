package com.controlleads.auth;

/**
 * Delivery seam for the password-reset email. The default logs the reset link;
 * provide your own bean (Spring Mail / a transactional email provider) to send
 * for real without touching the auth flow.
 */
public interface PasswordResetMailer {

    void sendResetLink(String email, String rawToken);
}
