package com.rwashift.platform.units.iam.api.dto;

import com.rwashift.platform.units.iam.domain.model.PlatformRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrganizationUserRequest(
        @NotBlank @Email String email,
        @NotBlank String displayName,
        @NotNull PlatformRole role) {
}
