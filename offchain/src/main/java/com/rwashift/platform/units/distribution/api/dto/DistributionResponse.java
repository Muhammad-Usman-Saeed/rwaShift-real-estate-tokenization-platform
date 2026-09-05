package com.rwashift.platform.units.distribution.api.dto;

import com.rwashift.platform.units.distribution.domain.model.Distribution;
import java.math.BigDecimal;
import java.time.Instant;

public record DistributionResponse(
        String id,
        String organizationId,
        String offeringId,
        BigDecimal totalAmount,
        String currency,
        Instant recordDate,
        String status
) {
    public static DistributionResponse from(Distribution d) {
        return new DistributionResponse(d.getId(), d.getOrganizationId(), d.getOfferingId(), d.getTotalAmount(),
                d.getCurrency(), d.getRecordDate(), d.getStatus().name());
    }
}
