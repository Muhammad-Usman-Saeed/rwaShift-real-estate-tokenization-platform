package com.rwashift.platform.units.organization.api.dto;

import com.rwashift.platform.units.organization.domain.model.Organization;
import java.time.Instant;

public record OrganizationResponse(
        String id,
        String legalName,
        String displayName,
        String countryCode,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getLegalName(),
                organization.getDisplayName(),
                organization.getCountryCode(),
                organization.getStatus().name(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}
