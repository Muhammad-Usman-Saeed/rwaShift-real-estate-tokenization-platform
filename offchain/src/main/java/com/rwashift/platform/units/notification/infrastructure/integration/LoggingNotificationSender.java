package com.rwashift.platform.units.notification.infrastructure.integration;

import com.rwashift.platform.units.notification.application.port.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String recipientUserId, String subject, String message) {
        log.info("Notification to user={} subject=\"{}\": {}", recipientUserId, subject, message);
    }
}
