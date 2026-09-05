package com.rwashift.platform.units.kyc.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * NOT_STARTED -> SUBMITTED -> UNDER_REVIEW -> VERIFIED or REJECTED. {@code REJECTED} may be
 * resubmitted ({@code REJECTED -> SUBMITTED}), a practical necessity not called out explicitly
 * in the base state machine but required for a workable compliance workflow.
 */
public enum KycStatus {
    NOT_STARTED,
    SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED;

    private static final Map<KycStatus, Set<KycStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(KycStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(NOT_STARTED, EnumSet.of(SUBMITTED));
        ALLOWED_TRANSITIONS.put(SUBMITTED, EnumSet.of(UNDER_REVIEW));
        ALLOWED_TRANSITIONS.put(UNDER_REVIEW, EnumSet.of(VERIFIED, REJECTED));
        ALLOWED_TRANSITIONS.put(VERIFIED, EnumSet.noneOf(KycStatus.class));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.of(SUBMITTED));
    }

    public boolean canTransitionTo(KycStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
