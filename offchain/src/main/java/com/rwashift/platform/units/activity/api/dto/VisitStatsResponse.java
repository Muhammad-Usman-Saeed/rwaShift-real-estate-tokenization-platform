package com.rwashift.platform.units.activity.api.dto;

public record VisitStatsResponse(
        long visitorsToday,
        long visitorsThisWeek,
        long visitorsThisMonth,
        long visitorsThisYear,
        long investorsToday,
        long investorsThisWeek,
        long investorsThisMonth,
        long investorsThisYear) {
}
