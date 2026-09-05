package com.rwashift.platform.shared.security;

/**
 * Raised whenever a caller's organization does not match the organization that owns the
 * resource being accessed. Mapped to HTTP 403 with an RFC 9457 problem-detail body by the
 * global exception handler — never leaks whether the resource exists under a different org.
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String callerOrganizationId, String resourceOrganizationId) {
        super("Caller organization '%s' may not access a resource owned by a different organization"
                .formatted(callerOrganizationId));
    }
}
