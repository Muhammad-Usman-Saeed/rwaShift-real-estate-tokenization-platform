package com.rwashift.platform.units.activity.api.dto;

import java.time.Instant;

public record ActivityFeedItem(
        String id,
        String action,
        String resourceType,
        String message,
        Instant occurredAt) {
}
