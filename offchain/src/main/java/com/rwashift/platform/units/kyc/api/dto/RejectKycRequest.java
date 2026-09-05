package com.rwashift.platform.units.kyc.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectKycRequest(@NotBlank @Size(max = 1000) String reason) {
}
