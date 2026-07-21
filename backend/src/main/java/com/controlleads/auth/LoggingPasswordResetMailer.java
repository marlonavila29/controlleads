package com.controlleads.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default mailer — logs the reset link instead of emailing it. Fine for dev;
 * replace with a real sender in production (see AuthMailConfig).
 */
public class LoggingPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);

    private final String resetUrlBase;

    public LoggingPasswordResetMailer(String resetUrlBase) {
        this.resetUrlBase = resetUrlBase;
    }

    @Override
    public void sendResetLink(String email, String rawToken) {
        log.info("password reset requested for {} — link: {}{}", email, resetUrlBase, rawToken);
    }
}
