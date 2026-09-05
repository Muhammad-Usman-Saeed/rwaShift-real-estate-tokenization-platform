package com.rwashift.platform.bootstrap;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.command.CreateAssetCommand;
import com.rwashift.platform.units.asset.application.service.AssetApplicationService;
import com.rwashift.platform.units.asset.domain.model.Asset;
import com.rwashift.platform.units.asset.domain.model.AssetType;
import com.rwashift.platform.units.iam.application.service.UserProvisioningService;
import com.rwashift.platform.units.iam.domain.model.PlatformRole;
import com.rwashift.platform.units.investor.application.command.OnboardInvestorCommand;
import com.rwashift.platform.units.investor.application.service.InvestorApplicationService;
import com.rwashift.platform.units.investor.domain.model.InvestorType;
import com.rwashift.platform.units.kyc.application.service.KycApplicationService;
import com.rwashift.platform.units.legalstructure.application.command.CreateLegalStructureCommand;
import com.rwashift.platform.units.legalstructure.application.service.LegalStructureApplicationService;
import com.rwashift.platform.units.legalstructure.domain.model.InvestmentInstrumentType;
import com.rwashift.platform.units.legalstructure.domain.model.LegalEntityType;
import com.rwashift.platform.units.legalstructure.domain.model.RelationshipToAsset;
import com.rwashift.platform.units.offering.application.command.CreateOfferingCommand;
import com.rwashift.platform.units.offering.application.service.OfferingApplicationService;
import com.rwashift.platform.units.organization.application.command.CreateOrganizationCommand;
import com.rwashift.platform.units.organization.application.service.OrganizationApplicationService;
import com.rwashift.platform.units.organization.domain.model.Organization;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo scenario from the product spec: "ABC Real Estate" issuer, "Dubai Business
 * Tower" asset ($10,000,000), "Dubai Business Tower SPV Ltd." legal structure, a $2,000,000 /
 * 20,000-unit / $100 offering with a $5,000 minimum, Investor A (KYC-verified) and Investor B
 * (deliberately left unverified).
 *
 * <p>Only enabled via {@code rwashift.seed.enabled=true} (see {@code docker-compose.yml}), never
 * on a normal boot or during tests. Goes as far as off-chain state allows: the offering is left
 * {@code APPROVED} rather than driven through on-chain tokenization here, since that requires a
 * live chain with the `onchain/` project's contracts already deployed — see README for the
 * follow-up API calls (`begin-tokenization` -> Tokenization's `/deploy` -> `investments` ->
 * `confirm-payment`) that complete the flow against a real Anvil instance.
 */
@Component
@ConditionalOnProperty(name = "rwashift.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String PLATFORM_ADMIN_EMAIL = "platform-admin@rwashift.com";

    private final UserProvisioningService userProvisioningService;
    private final OrganizationApplicationService organizationApplicationService;
    private final AssetApplicationService assetApplicationService;
    private final LegalStructureApplicationService legalStructureApplicationService;
    private final OfferingApplicationService offeringApplicationService;
    private final InvestorApplicationService investorApplicationService;
    private final KycApplicationService kycApplicationService;
    private final PlatformUserRepository platformUserRepository;

    public DemoDataSeeder(UserProvisioningService userProvisioningService, OrganizationApplicationService organizationApplicationService,
            AssetApplicationService assetApplicationService, LegalStructureApplicationService legalStructureApplicationService,
            OfferingApplicationService offeringApplicationService, InvestorApplicationService investorApplicationService,
            KycApplicationService kycApplicationService, PlatformUserRepository platformUserRepository) {
        this.userProvisioningService = userProvisioningService;
        this.organizationApplicationService = organizationApplicationService;
        this.assetApplicationService = assetApplicationService;
        this.legalStructureApplicationService = legalStructureApplicationService;
        this.offeringApplicationService = offeringApplicationService;
        this.investorApplicationService = investorApplicationService;
        this.kycApplicationService = kycApplicationService;
        this.platformUserRepository = platformUserRepository;
    }

    @Override
    public void run(String... args) {
        // Idempotent across restarts: rwashift.seed.enabled=true is meant to be safely left on
        // (it's the aggregated docker-compose.yml's default), and a container restart against
        // the same database — a normal Docker occurrence, not an error condition — must not
        // crash trying to re-insert the same demo user.
        if (platformUserRepository.existsByEmail(PLATFORM_ADMIN_EMAIL)) {
            log.info("Demo data already present (found {}) — skipping seed.", PLATFORM_ADMIN_EMAIL);
            return;
        }

        var platformAdmin = userProvisioningService.createGlobalUser(
                PLATFORM_ADMIN_EMAIL, "ChangeMe123!", "Platform Admin", PlatformRole.PLATFORM_ADMIN);
        TenantContext platformAdminCtx = new TenantContext(platformAdmin.getId(), null, Set.of("PLATFORM_ADMIN"), null);

        Organization organization = organizationApplicationService.createOrganization(platformAdminCtx,
                new CreateOrganizationCommand("ABC Real Estate LLC", "ABC Real Estate", "AE"));

        var issuerAdmin = userProvisioningService.createOrganizationUser("issuer-admin@abcrealestate.com",
                "ChangeMe123!", "ABC Issuer Admin", organization.getId(), PlatformRole.ISSUER_ADMIN);
        var complianceOfficer = userProvisioningService.createOrganizationUser("compliance@abcrealestate.com",
                "ChangeMe123!", "ABC Compliance Officer", organization.getId(), PlatformRole.COMPLIANCE_OFFICER);

        TenantContext issuerCtx = new TenantContext(issuerAdmin.getId(), organization.getId(), Set.of("ISSUER_ADMIN"), null);
        TenantContext complianceCtx =
                new TenantContext(complianceOfficer.getId(), organization.getId(), Set.of("COMPLIANCE_OFFICER"), null);

        Asset asset = assetApplicationService.createAsset(issuerCtx, new CreateAssetCommand(
                "Dubai Business Tower", AssetType.COMMERCIAL, "Prime commercial tower in Dubai",
                "Dubai, UAE", new BigDecimal("10000000.00"), "USD"));
        assetApplicationService.markUnderVerification(issuerCtx, asset.getId());
        assetApplicationService.markVerified(complianceCtx, asset.getId());

        var legalStructure = legalStructureApplicationService.createLegalStructure(issuerCtx,
                new CreateLegalStructureCommand(asset.getId(), "Dubai Business Tower SPV Ltd.", LegalEntityType.SPV,
                        "DIFC, UAE", "SPV-2026-001", RelationshipToAsset.FULL_OWNER, InvestmentInstrumentType.EQUITY_UNITS));
        legalStructureApplicationService.activate(issuerCtx, legalStructure.getId());

        var offering = offeringApplicationService.createOffering(issuerCtx, new CreateOfferingCommand(
                asset.getId(), legalStructure.getId(), "Dubai Business Tower SPV Units", new BigDecimal("2000000.00"),
                "USD", 20_000, new BigDecimal("100.00"), new BigDecimal("5000.00"), new BigDecimal("20.0000"),
                Instant.now(), Instant.now().plusSeconds(90L * 24 * 3600)));
        offeringApplicationService.submitForReview(issuerCtx, offering.getId());
        offeringApplicationService.approve(complianceCtx, offering.getId());

        var investorAUser = userProvisioningService.createGlobalUser(
                "investor-a@example.com", "ChangeMe123!", "Investor A", null);
        // Anvil's well-known default account #1 (funded, publicly documented private key
        // 0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690) — a real, usable
        // wallet address rather than an arbitrary placeholder, so it can actually be imported
        // into a wallet for the demo.
        var investorA = investorApplicationService.onboardInvestor(new OnboardInvestorCommand(investorAUser.getId(),
                InvestorType.INDIVIDUAL, "Investor A", "AE", java.time.LocalDate.of(1985, 1, 1), null,
                "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"));
        kycApplicationService.submit(investorA.getId());
        var kycCaseA = kycApplicationService.getByInvestor(investorA.getId());
        kycApplicationService.startReview(kycCaseA.getId());
        kycApplicationService.verify(kycCaseA.getId(), complianceOfficer.getId());

        var investorBUser = userProvisioningService.createGlobalUser(
                "investor-b@example.com", "ChangeMe123!", "Investor B", null);
        // Anvil's well-known default account #2 (private key
        // 0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365).
        investorApplicationService.onboardInvestor(new OnboardInvestorCommand(investorBUser.getId(), InvestorType.INDIVIDUAL,
                "Investor B", "AE", java.time.LocalDate.of(1990, 1, 1), null,
                "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"));
        // Investor B is intentionally left with KYC NOT_STARTED / ineligible for the
        // "unverified transfer rejected, then succeeds once verified" acceptance demo.

        log.info("Demo data seeded. Organization={} Offering={} InvestorA={} InvestorB={}",
                organization.getId(), offering.getId(), investorA.getId(), investorBUser.getId());
        log.info("Next steps to reach on-chain state: POST /api/v1/offerings/{}/begin-tokenization, "
                + "POST /api/v1/tokenization/offerings/{}/deploy, then create an investment and confirm payment.",
                offering.getId(), offering.getId());
    }
}
