package com.rwashift.platform.units.iam.api.dto;

import java.util.Set;

public record CurrentUserResponse(
        String userId,
        String organizationId,
        Set<String> roles,
        String investorId
) {
}
