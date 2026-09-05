package com.rwashift.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.command.CreateAssetCommand;
import com.rwashift.platform.units.asset.application.service.AssetApplicationService;
import com.rwashift.platform.units.asset.domain.model.AssetStatus;
import com.rwashift.platform.units.asset.domain.model.AssetType;
import com.rwashift.platform.units.audit.application.service.AuditApplicationService;
import com.rwashift.platform.units.iam.application.service.UserProvisioningService;
import com.rwashift.platform.units.investor.application.command.OnboardInvestorCommand;
import com.rwashift.platform.units.investor.application.service.InvestorApplicationService;
import com.rwashift.platform.units.investor.domain.model.InvestorType;
import com.rwashift.platform.units.kyc.application.service.KycApplicationService;
import com.rwashift.platform.units.kyc.domain.model.KycStatus;
import com.rwashift.platform.units.legalstructure.application.command.CreateLegalStructureCommand;
import com.rwashift.platform.units.legalstructure.application.service.LegalStructureApplicationService;
import com.rwashift.platform.units.legalstructure.domain.model.InvestmentInstrumentType;
import com.rwashift.platform.units.legalstructure.domain.model.LegalEntityType;
import com.rwashift.platform.units.legalstructure.domain.model.RelationshipToAsset;
import com.rwashift.platform.units.offering.application.command.CreateOfferingCommand;
import com.rwashift.platform.units.offering.application.service.OfferingApplicationService;
import com.rwashift.platform.units.offering.domain.model.OfferingStatus;
import com.rwashift.platform.units.organization.application.command.CreateOrganizationCommand;
import com.rwashift.platform.units.organization.application.service.OrganizationApplicationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Drives the required end-to-end scenario (section 29 of the product spec) as far as it can go
 * without a live blockchain: organization -> asset -> legal structure -> offering -> approve ->
 * investor -> KYC -> audit trail. Deliberately stops short of
 * {@code ComplianceApplicationService.evaluateEligibility}: a newly-eligible decision always
 * triggers on-chain identity registration via {@code IdentityRegistrationPort} ->
 * {@code TokenizationApplicationService}, which requires a real deployed offering token — that
 * behavior (rule evaluation + the identity-registration trigger, with ports mocked) is covered
 * by {@code ComplianceApplicationServiceTest} instead. On-chain identity registration, unit
 * issuance, ownership projection, and distribution settlement are covered by the separate
 * `onchain/` Foundry test suite (unit/integration/fuzz/invariant) plus this repo's
 * {@code TokenizationApplicationService}/{@code BlockchainTransactionManager} being exercised
 * against a real Anvil instance via the manual flow documented in the README.
 */
class OffChainLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationApplicationService organizationApplicationService;
    @Autowired
    private AssetApplicationService assetApplicationService;
    @Autowired
    private LegalStructureApplicationService legalStructureApplicationService;
    @Autowired
    private OfferingApplicationService offeringApplicationService;
    @Autowired
    private InvestorApplicationService investorApplicationService;
    @Autowired
    private UserProvisioningService userProvisioningService;
    @Autowired
    private KycApplicationService kycApplicationService;
    @Autowired
    private AuditApplicationService auditApplicationService;

    @Test
    void fullOffChainFlowReachesOpenOfferingAndEligibleInvestor() {
        TenantContext platformAdmin = new TenantContext("platform-admin", null, Set.of("PLATFORM_ADMIN"), null);
        var organization = organizationApplicationService.createOrganization(platformAdmin,
                new CreateOrganizationCommand("ABC Real Estate LLC", "ABC Real Estate", "AE"));

        TenantContext issuerAdmin = new TenantContext("issuer-admin", organization.getId(), Set.of("ISSUER_ADMIN"), null);
        TenantContext complianceOfficer =
                new TenantContext("compliance-officer", organization.getId(), Set.of("COMPLIANCE_OFFICER"), null);

        var asset = assetApplicationService.createAsset(issuerAdmin, new CreateAssetCommand(
                "Dubai Business Tower", AssetType.COMMERCIAL, "Prime tower", "Dubai, UAE",
                new BigDecimal("10000000.00"), "USD"));
        assetApplicationService.markUnderVerification(issuerAdmin, asset.getId());
        assetApplicationService.markVerified(complianceOfficer, asset.getId());
        assertThat(assetApplicationService.getAsset(issuerAdmin, asset.getId()).getStatus()).isEqualTo(AssetStatus.VERIFIED);

        var legalStructure = legalStructureApplicationService.createLegalStructure(issuerAdmin,
                new CreateLegalStructureCommand(asset.getId(), "Dubai Business Tower SPV Ltd.", LegalEntityType.SPV,
                        "DIFC, UAE", "SPV-1", RelationshipToAsset.FULL_OWNER, InvestmentInstrumentType.EQUITY_UNITS));
        legalStructureApplicationService.activate(issuerAdmin, legalStructure.getId());

        var offering = offeringApplicationService.createOffering(issuerAdmin, new CreateOfferingCommand(
                asset.getId(), legalStructure.getId(), "Dubai Business Tower SPV Units", new BigDecimal("2000000.00"),
                "USD", 20_000, new BigDecimal("100.00"), new BigDecimal("5000.00"), new BigDecimal("20.0000"),
                Instant.now(), Instant.now().plusSeconds(3600)));
        offeringApplicationService.submitForReview(issuerAdmin, offering.getId());
        offeringApplicationService.approve(complianceOfficer, offering.getId());
        assertThat(offeringApplicationService.getOffering(issuerAdmin, offering.getId()).getStatus())
                .isEqualTo(OfferingStatus.APPROVED);

        // Simulate tokenization confirming (normally driven by Tokenization against a live chain).
        offeringApplicationService.beginTokenization(issuerAdmin, offering.getId());
        offeringApplicationService.onTokenizationConfirmed(offering.getId());
        assertThat(offeringApplicationService.getOffering(issuerAdmin, offering.getId()).getStatus())
                .isEqualTo(OfferingStatus.OPEN);

        var investorUserAccount = userProvisioningService.createGlobalUser(
                "investor-a@example.com", "ChangeMe123!", "Investor A", null);
        var investor = investorApplicationService.onboardInvestor(new OnboardInvestorCommand(investorUserAccount.getId(),
                InvestorType.INDIVIDUAL, "Investor A", "AE", LocalDate.of(1985, 1, 1), null,
                "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"));

        kycApplicationService.submit(investor.getId());
        var kycCase = kycApplicationService.getByInvestor(investor.getId());
        kycApplicationService.startReview(kycCase.getId());
        kycApplicationService.verify(kycCase.getId(), "compliance-officer");
        assertThat(kycApplicationService.getByInvestor(investor.getId()).getStatus()).isEqualTo(KycStatus.VERIFIED);

        // Audit trail recorded the offering approval and the KYC verification.
        var offeringAudit = auditApplicationService.getForResource("Offering", offering.getId());
        assertThat(offeringAudit).anyMatch(e -> "OFFERING_APPROVED".equals(e.getAction()));
        var kycAudit = auditApplicationService.getForResource("KycCase", kycCase.getId());
        assertThat(kycAudit).anyMatch(e -> "KYC_VERIFIED".equals(e.getAction()));
    }
}
