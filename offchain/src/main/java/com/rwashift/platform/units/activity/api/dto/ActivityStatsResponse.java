package com.rwashift.platform.units.activity.api.dto;

import java.util.Map;

/**
 * {@code totalCount} is always the sum of {@code countByCategory}'s values — both are computed
 * from the same newsworthy-event set (see {@code ActivityApplicationService#getStats}), just
 * total vs. broken down by the event's resource type (Offering, Investment, Asset, ...).
 */
public record ActivityStatsResponse(long totalCount, Map<String, Long> countByCategory) {
}
