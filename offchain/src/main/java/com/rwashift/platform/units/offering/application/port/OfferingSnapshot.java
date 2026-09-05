package com.rwashift.platform.units.offering.application.port;

import java.math.BigDecimal;

/**
 * Read-only cross-unit view of an Offering's investable terms, exposed to Investment/Compliance
 * without leaking the {@code Offering} JPA entity itself.
 */
public record OfferingSnapshot(
        String offeringId,
        String organizationId,
        String legalStructureId,
        String name,
        String currency,
        BigDecimal unitPrice,
        BigDecimal minimumInvestment,
        long totalUnits,
        long unitsAvailable,
        boolean openForInvestment
) {
}
