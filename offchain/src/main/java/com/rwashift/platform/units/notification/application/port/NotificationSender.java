package com.rwashift.platform.units.notification.application.port;

/**
 * V1 does not implement real email/SMS delivery — {@code LoggingNotificationSender} just logs.
 * The abstraction exists so a real provider (SES, Twilio, ...) can be dropped in later without
 * touching any calling unit.
 */
public interface NotificationSender {

    void send(String recipientUserId, String subject, String message);
}
