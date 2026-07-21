package com.controlleads.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    /** In-app-only default; a real push/email dispatcher bean overrides it. */
    @Bean
    @ConditionalOnMissingBean(NotificationDispatcher.class)
    NotificationDispatcher notificationDispatcher() {
        return new LoggingNotificationDispatcher();
    }
}
