package com.rwashift.platform.units.investment.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * INITIATED -> ELIGIBILITY_PENDING -> PAYMENT_PENDING -> CONFIRMED -> TOKEN_ISSUANCE_PENDING ->
 * SETTLED, with a terminal failure state reachable from each waiting state
 * ({@code ELIGIBILITY_REJECTED}, {@code PAYMENT_FAILED}, {@code TOKEN_ISSUANCE_FAILED}).
 *
 * <p>{@code CANCELLED} is the investor's own self-service exit, reachable only from the two
 * pre-payment-confirmation states ({@code ELIGIBILITY_PENDING}, {@code PAYMENT_PENDING}) —
 * nothing has moved on-chain yet at that point, so there's nothing to unwind. Once payment is
 * {@code CONFIRMED} the only way out is the existing failure states (a settled/in-flight
 * commitment isn't something the investor can simply take back).
 */
public enum InvestmentStatus {
    INITIATED,
    ELIGIBILITY_PENDING,
    ELIGIBILITY_REJECTED,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    CONFIRMED,
    TOKEN_ISSUANCE_PENDING,
    TOKEN_ISSUANCE_FAILED,
    SETTLED,
    CANCELLED;

    private static final Map<InvestmentStatus, Set<InvestmentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(InvestmentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(INITIATED, EnumSet.of(ELIGIBILITY_PENDING));
        ALLOWED_TRANSITIONS.put(ELIGIBILITY_PENDING, EnumSet.of(PAYMENT_PENDING, ELIGIBILITY_REJECTED, CANCELLED));
        ALLOWED_TRANSITIONS.put(ELIGIBILITY_REJECTED, EnumSet.noneOf(InvestmentStatus.class));
        ALLOWED_TRANSITIONS.put(PAYMENT_PENDING, EnumSet.of(CONFIRMED, PAYMENT_FAILED, CANCELLED));
        ALLOWED_TRANSITIONS.put(PAYMENT_FAILED, EnumSet.noneOf(InvestmentStatus.class));
        ALLOWED_TRANSITIONS.put(CONFIRMED, EnumSet.of(TOKEN_ISSUANCE_PENDING));
        ALLOWED_TRANSITIONS.put(TOKEN_ISSUANCE_PENDING, EnumSet.of(SETTLED, TOKEN_ISSUANCE_FAILED));
        ALLOWED_TRANSITIONS.put(TOKEN_ISSUANCE_FAILED, EnumSet.noneOf(InvestmentStatus.class));
        ALLOWED_TRANSITIONS.put(SETTLED, EnumSet.noneOf(InvestmentStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(InvestmentStatus.class));
    }

    public boolean canTransitionTo(InvestmentStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
