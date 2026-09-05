package com.rwashift.platform.units.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rwashift.platform.AbstractIntegrationTest;
import com.rwashift.platform.shared.security.TenantAccessDeniedException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.command.CreateAssetCommand;
import com.rwashift.platform.units.asset.application.service.AssetApplicationService;
import com.rwashift.platform.units.asset.domain.model.AssetType;
import com.rwashift.platform.units.organization.application.command.CreateOrganizationCommand;
import com.rwashift.platform.units.organization.application.service.OrganizationApplicationService;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the core multi-tenancy guarantee: an issuer from Organization A can never read or
 * write a resource that belongs to Organization B, even knowing its id (see ADR notes and
 * {@link com.rwashift.platform.shared.security.TenantContext#requireSameOrganization}).
 */
class TenantIsolationIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationApplicationService organizationApplicationService;
    @Autowired
    private AssetApplicationService assetApplicationService;

    private static final TenantContext PLATFORM_ADMIN = new TenantContext("platform-admin", null, Set.of("PLATFORM_ADMIN"), null);

    @Test
    void organizationACannotReadOrganizationBsAsset() {
        var orgA = organizationApplicationService.createOrganization(PLATFORM_ADMIN,
                new CreateOrganizationCommand("Org A Ltd", "Org A", "AE"));
        var orgB = organizationApplicationService.createOrganization(PLATFORM_ADMIN,
                new CreateOrganizationCommand("Org B Ltd", "Org B", "US"));

        TenantContext orgAIssuer = new TenantContext("user-a", orgA.getId(), Set.of("ISSUER_ADMIN"), null);
        TenantContext orgBIssuer = new TenantContext("user-b", orgB.getId(), Set.of("ISSUER_ADMIN"), null);

        var assetB = assetApplicationService.createAsset(orgBIssuer, new CreateAssetCommand(
                "Org B Tower", AssetType.COMMERCIAL, "desc", "Somewhere", new BigDecimal("1000000.00"), "USD"));

        assertThatThrownBy(() -> assetApplicationService.getAsset(orgAIssuer, assetB.getId()))
                .isInstanceOf(TenantAccessDeniedException.class);

        // Org B can still read its own asset.
        assertThat(assetApplicationService.getAsset(orgBIssuer, assetB.getId()).getId()).isEqualTo(assetB.getId());
    }

    @Test
    void organizationACannotUpdateOrganizationBsAsset() {
        var orgA = organizationApplicationService.createOrganization(PLATFORM_ADMIN,
                new CreateOrganizationCommand("Org A2 Ltd", "Org A2", "AE"));
        var orgB = organizationApplicationService.createOrganization(PLATFORM_ADMIN,
                new CreateOrganizationCommand("Org B2 Ltd", "Org B2", "US"));
        TenantContext orgAIssuer = new TenantContext("user-a2", orgA.getId(), Set.of("ISSUER_ADMIN"), null);
        TenantContext orgBIssuer = new TenantContext("user-b2", orgB.getId(), Set.of("ISSUER_ADMIN"), null);

        var assetB = assetApplicationService.createAsset(orgBIssuer, new CreateAssetCommand(
                "Org B2 Tower", AssetType.COMMERCIAL, "desc", "Somewhere", new BigDecimal("500000.00"), "USD"));

        assertThatThrownBy(() -> assetApplicationService.updateAsset(orgAIssuer, assetB.getId(),
                new com.rwashift.platform.units.asset.application.command.UpdateAssetCommand(
                        "Hacked name", "desc", "Nowhere", BigDecimal.ONE, "USD")))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    void platformAdminCanReadAnyOrganizationsAsset() {
        var orgC = organizationApplicationService.createOrganization(PLATFORM_ADMIN,
                new CreateOrganizationCommand("Org C Ltd", "Org C", "GB"));
        TenantContext orgCIssuer = new TenantContext("user-c", orgC.getId(), Set.of("ISSUER_ADMIN"), null);
        var assetC = assetApplicationService.createAsset(orgCIssuer, new CreateAssetCommand(
                "Org C Tower", AssetType.RESIDENTIAL, "desc", "Somewhere", new BigDecimal("250000.00"), "USD"));

        assertThat(assetApplicationService.getAsset(PLATFORM_ADMIN, assetC.getId()).getId()).isEqualTo(assetC.getId());
    }
}
