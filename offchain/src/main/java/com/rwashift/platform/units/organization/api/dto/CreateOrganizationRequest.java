package com.rwashift.platform.units.organization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 300) String legalName,
        @NotBlank @Size(max = 200) String displayName,
        @Size(min = 2, max = 2) String countryCode
) {
}
