package com.rwashift.platform.units.iam.api.dto;

/**
 * {@code temporaryPassword} appears here, and only here, exactly once — see
 * {@code TemporaryPasswordGenerator}'s Javadoc. The list endpoint ({@link OrganizationUserResponse})
 * deliberately has no password field.
 */
public record CreateOrganizationUserResponse(
        String userId,
        String email,
        String displayName,
        String role,
        String temporaryPassword) {
}
