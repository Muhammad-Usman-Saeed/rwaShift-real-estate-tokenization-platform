package com.rwashift.platform.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @Test
    void sameOrganizationIsAllowed() {
        TenantContext ctx = new TenantContext("user-1", "org-A", Set.of("ISSUER_ADMIN"), null);
        assertThatCode(() -> ctx.requireSameOrganization("org-A")).doesNotThrowAnyException();
    }

    @Test
    void differentOrganizationIsDenied() {
        TenantContext ctx = new TenantContext("user-1", "org-A", Set.of("ISSUER_ADMIN"), null);
        assertThatThrownBy(() -> ctx.requireSameOrganization("org-B"))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    void platformAdminBypassesTenantCheck() {
        TenantContext ctx = new TenantContext("admin-1", null, Set.of("PLATFORM_ADMIN"), null);
        assertThatCode(() -> ctx.requireSameOrganization("any-org")).doesNotThrowAnyException();
    }

    @Test
    void nullOrganizationIsDeniedForNonAdmin() {
        TenantContext ctx = new TenantContext("investor-1", null, Set.of("INVESTOR"), "investor-id");
        assertThatThrownBy(() -> ctx.requireSameOrganization("org-A"))
                .isInstanceOf(TenantAccessDeniedException.class);
    }
}
