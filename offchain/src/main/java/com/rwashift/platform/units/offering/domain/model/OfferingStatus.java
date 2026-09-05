package com.rwashift.platform.units.offering.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * DRAFT -> UNDER_REVIEW -> APPROVED -> TOKENIZING -> OPEN -> FUNDED -> CLOSED.
 * {@link #REJECTED} is a terminal failure state reachable only from {@code UNDER_REVIEW}.
 * {@code CLOSED} is also reachable directly from {@code OPEN} (early close) and from
 * {@code FUNDED} (normal close) — see the allowed-transition table below.
 */
public enum OfferingStatus {
    DRAFT,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    TOKENIZING,
    OPEN,
    FUNDED,
    CLOSED;

    private static final Map<OfferingStatus, Set<OfferingStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OfferingStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DRAFT, EnumSet.of(UNDER_REVIEW));
        ALLOWED_TRANSITIONS.put(UNDER_REVIEW, EnumSet.of(APPROVED, REJECTED));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.of(TOKENIZING));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.noneOf(OfferingStatus.class));
        ALLOWED_TRANSITIONS.put(TOKENIZING, EnumSet.of(OPEN));
        ALLOWED_TRANSITIONS.put(OPEN, EnumSet.of(FUNDED, CLOSED));
        ALLOWED_TRANSITIONS.put(FUNDED, EnumSet.of(CLOSED));
        ALLOWED_TRANSITIONS.put(CLOSED, EnumSet.noneOf(OfferingStatus.class));
    }

    public boolean canTransitionTo(OfferingStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
