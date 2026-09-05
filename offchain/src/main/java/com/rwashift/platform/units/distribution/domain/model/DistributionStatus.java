package com.rwashift.platform.units.distribution.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** DRAFT -> CALCULATED -> APPROVED -> PROCESSING -> COMPLETED. */
public enum DistributionStatus {
    DRAFT,
    CALCULATED,
    APPROVED,
    PROCESSING,
    COMPLETED;

    private static final Map<DistributionStatus, Set<DistributionStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(DistributionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DRAFT, EnumSet.of(CALCULATED));
        ALLOWED_TRANSITIONS.put(CALCULATED, EnumSet.of(APPROVED));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.of(PROCESSING));
        ALLOWED_TRANSITIONS.put(PROCESSING, EnumSet.of(COMPLETED));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(DistributionStatus.class));
    }

    public boolean canTransitionTo(DistributionStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
