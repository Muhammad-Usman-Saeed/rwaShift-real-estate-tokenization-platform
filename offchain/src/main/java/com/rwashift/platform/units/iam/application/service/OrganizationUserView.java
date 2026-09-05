package com.rwashift.platform.units.iam.application.service;

import com.rwashift.platform.units.iam.domain.model.PlatformRole;
import com.rwashift.platform.units.iam.domain.model.PlatformUserStatus;

/** A {@code PlatformUser} joined with their role in one specific organization — see {@link UserProvisioningService#listOrganizationUsers}. */
public record OrganizationUserView(String userId, String email, String displayName, PlatformRole role, PlatformUserStatus status) {
}
