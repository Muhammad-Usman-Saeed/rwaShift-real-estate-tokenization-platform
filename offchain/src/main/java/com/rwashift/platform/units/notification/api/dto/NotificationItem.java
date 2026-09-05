package com.rwashift.platform.units.notification.api.dto;

import java.time.Instant;

/**
 * One actionable-or-informational item in the caller's notification list. {@code id} is a stable,
 * derived string (e.g. {@code "offering-approval:<offeringId>"}) — there is no persisted
 * notification row (see {@link com.rwashift.platform.units.notification.application.service.NotificationApplicationService}'s
 * Javadoc), so the frontend uses this id as the key for its own "seen" tracking (localStorage)
 * rather than the backend tracking read/unread state.
 */
public record NotificationItem(
        String id,
        String type,
        String title,
        String message,
        String link,
        Instant occurredAt) {
}
