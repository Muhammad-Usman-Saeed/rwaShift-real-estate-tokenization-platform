package com.rwashift.platform.shared.security;

import java.util.Set;

/**
 * The authenticated principal's tenant boundary for the current request: which organization
 * they act as, which roles they hold, and (for investors) their own investor id. Every
 * unit's application services take this as an explicit parameter for tenant-scoped operations
 * rather than reading a thread-local implicitly, so tenant checks stay visible in code and are
 * trivial to unit-test (see the tenant-isolation test suite).
 */
public record TenantContext(
        String userId,
        String organizationId,
        Set<String> roles,
        String investorId
) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isPlatformAdmin() {
        return hasRole("PLATFORM_ADMIN");
    }

    /**
     * Throws unless the given resource's organization matches this context's organization, or
     * the caller is a platform admin. This is the single choke point every unit's application
     * service should call before reading/writing an organization-scoped aggregate.
     */
    public void requireSameOrganization(String resourceOrganizationId) {
        if (isPlatformAdmin()) {
            return;
        }
        if (organizationId == null || !organizationId.equals(resourceOrganizationId)) {
            throw new TenantAccessDeniedException(organizationId, resourceOrganizationId);
        }
    }
}
