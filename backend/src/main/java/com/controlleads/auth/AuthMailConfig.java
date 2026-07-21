package com.controlleads.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthMailConfig {

    /** Log-only default; a real email sender bean overrides it. */
    @Bean
    @ConditionalOnMissingBean(PasswordResetMailer.class)
    PasswordResetMailer passwordResetMailer(
            @Value("${app.web.password-reset-url:http://localhost:4200/reset-password?token=}") String base) {
        return new LoggingPasswordResetMailer(base);
    }
}
