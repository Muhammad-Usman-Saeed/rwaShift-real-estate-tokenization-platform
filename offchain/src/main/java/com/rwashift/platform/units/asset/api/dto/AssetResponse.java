package com.rwashift.platform.units.asset.api.dto;

import com.rwashift.platform.units.asset.domain.model.Asset;
import java.math.BigDecimal;
import java.time.Instant;

public record AssetResponse(
        String id,
        String organizationId,
        String name,
        String type,
        String description,
        String location,
        BigDecimal valuation,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getOrganizationId(),
                asset.getName(),
                asset.getType().name(),
                asset.getDescription(),
                asset.getLocation(),
                asset.getValuation(),
                asset.getCurrency(),
                asset.getStatus().name(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }
}
