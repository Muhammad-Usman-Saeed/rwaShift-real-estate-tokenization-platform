package com.rwashift.platform.units.tokenization.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.investor.application.port.InvestorLookupPort;
import com.rwashift.platform.units.investor.application.port.InvestorSnapshot;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import com.rwashift.platform.units.offering.application.port.OfferingSnapshot;
import com.rwashift.platform.units.tokenization.application.port.DistributionRecordingPort;
import com.rwashift.platform.units.tokenization.application.port.IdentityRegistrationPort;
import com.rwashift.platform.units.tokenization.application.port.PaymentCollectionAddressPort;
import com.rwashift.platform.units.tokenization.application.port.TokenIssuancePort;
import com.rwashift.platform.units.tokenization.domain.model.BusinessReferenceType;
import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;
import com.rwashift.platform.units.tokenization.domain.model.ContractType;
import com.rwashift.platform.units.tokenization.domain.repository.BlockchainTransactionRepository;
import com.rwashift.platform.units.tokenization.domain.repository.ContractDeploymentRepository;
import com.rwashift.platform.units.tokenization.domain.repository.OrganizationWalletRepository;
import com.rwashift.platform.units.tokenization.infrastructure.configuration.BlockchainProperties;
import com.rwashift.platform.units.tokenization.infrastructure.integration.Web3jContractGateway;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;

/**
 * The only unit in the platform that talks to Web3j (directly, or transitively via
 * {@link Web3jContractGateway}/{@link BlockchainTransactionManager}). Consumes the ABIs/contract
 * behavior of the separate `onchain/` Foundry project (`RwaShiftTokenFactory`,
 * `RwaShiftIdentityGateway`, T-REX's `Token`) — never redefines that business logic here.
 *
 * <p>Each organization gets its own on-chain wallet ({@link OrganizationWalletProvisioningService}),
 * provisioned lazily on that organization's first tokenized offering. That wallet — not the
 * platform's shared agent key — becomes {@code issuerAdmin}/{@code TOKEN_AGENT} for every offering
 * the organization tokenizes, so a leaked platform agent key can no longer mint, burn, freeze, or
 * otherwise control any organization's already-deployed tokens (see docs/critical-analysis.md).
 * The platform agent key retains only {@code ISSUER_ADMIN_ROLE} on {@code RwaShiftTokenFactory} —
 * narrow, deliberate, and, per that contract's own documented trust boundary, incapable of
 * touching an already-deployed suite — plus investor identity registration and distribution
 * record-keeping, which are legitimately platform-wide compliance infrastructure, not organization
 * funds custody.
 */
@Service
public class TokenizationApplicationService
        implements IdentityRegistrationPort, TokenIssuancePort, DistributionRecordingPort, PaymentCollectionAddressPort {

    private final BlockchainTransactionManager transactionManager;
    private final ContractDeploymentRepository contractDeploymentRepository;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final Web3jContractGateway gateway;
    private final BlockchainProperties properties;
    private final OrganizationWalletProvisioningService walletProvisioningService;
    private final OrganizationWalletRepository organizationWalletRepository;
    private final OfferingLookupPort offeringLookupPort;
    private final InvestorLookupPort investorLookupPort;
    private final AuditPort auditPort;

    public TokenizationApplicationService(BlockchainTransactionManager transactionManager,
            ContractDeploymentRepository contractDeploymentRepository, BlockchainTransactionRepository blockchainTransactionRepository,
            Web3jContractGateway gateway, BlockchainProperties properties,
            OrganizationWalletProvisioningService walletProvisioningService, OrganizationWalletRepository organizationWalletRepository,
            OfferingLookupPort offeringLookupPort, InvestorLookupPort investorLookupPort, AuditPort auditPort) {
        this.transactionManager = transactionManager;
        this.contractDeploymentRepository = contractDeploymentRepository;
        this.blockchainTransactionRepository = blockchainTransactionRepository;
        this.gateway = gateway;
        this.organizationWalletRepository = organizationWalletRepository;
        this.properties = properties;
        this.walletProvisioningService = walletProvisioningService;
        this.offeringLookupPort = offeringLookupPort;
        this.investorLookupPort = investorLookupPort;
        this.auditPort = auditPort;
    }

    /**
     * The single trigger for an offering's on-chain deployment — drives the offering's own
     * {@code APPROVED -> TOKENIZING} transition itself (rather than requiring the caller to hit
     * the separate begin-tokenization endpoint first) so the state machine is guaranteed correct
     * by the time the async deployment-confirmation callback later drives {@code TOKENIZING ->
     * OPEN}; that later transition would otherwise fail every poll cycle forever if this offering
     * were still sitting in APPROVED.
     */
    @Transactional
    public void deployOfferingToken(String offeringId, String symbol) {
        OfferingSnapshot offering = offeringLookupPort.findSnapshot(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        offeringLookupPort.beginTokenization(offeringId);

        ContractDeployment deployment = new ContractDeployment(offeringId, properties.getNetwork(),
                properties.getChainId(), ContractType.TOKEN, null);
        contractDeploymentRepository.save(deployment);

        Credentials orgCredentials = walletProvisioningService.getOrCreateWallet(offering.organizationId());
        String orgWalletAddress = orgCredentials.getAddress();
        var transaction = transactionManager.createAndSubmit(BusinessReferenceType.OFFERING_DEPLOYMENT, offeringId,
                properties.getTokenFactoryAddress(), "createOffering",
                () -> gateway.createOffering(offeringId, offering.name(), symbol, 0, orgWalletAddress,
                        List.of(properties.getIdentityGatewayAddress()), List.of(orgWalletAddress)));
        auditPort.record(new AuditPort.AuditEntry(null, offering.organizationId(), "OFFERING_TOKEN_DEPLOYMENT_SUBMITTED",
                "Offering", offeringId, null, transaction.getStatus().name(), null, transaction.getTxHash()));
    }

    @Override
    @Transactional
    public void registerInvestorIdentity(String investorId, String offeringId) {
        String tokenAddress = resolveTokenAddress(offeringId);
        String identityRegistryAddress = gateway.identityRegistryOf(tokenAddress);
        InvestorSnapshot investor = investorLookupPort.findSnapshot(investorId)
                .orElseThrow(() -> new NotFoundException("Investor", investorId));
        if (investor.primaryWalletAddress() == null) {
            throw new DomainException("Investor has no primary wallet linked");
        }
        int countryCode = countryCodeOf(investor.countryCode());
        var transaction = transactionManager.createAndSubmit(BusinessReferenceType.IDENTITY_REGISTRATION,
                investorId + ":" + offeringId, properties.getIdentityGatewayAddress(), "registerInvestorIdentity",
                () -> gateway.registerInvestorIdentity(identityRegistryAddress, investor.primaryWalletAddress(),
                        countryCode, investorId + "-" + offeringId));
        auditPort.record(new AuditPort.AuditEntry(null, null, "IDENTITY_REGISTRATION_SUBMITTED", "Investor",
                investorId, null, transaction.getStatus().name(), null, transaction.getTxHash()));
    }

    @Override
    @Transactional
    public void requestUnitIssuance(String investmentId, String offeringId, String investorId, long units) {
        String tokenAddress = resolveTokenAddress(offeringId);
        InvestorSnapshot investor = investorLookupPort.findSnapshot(investorId)
                .orElseThrow(() -> new NotFoundException("Investor", investorId));
        OfferingSnapshot offering = offeringLookupPort.findSnapshot(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        Credentials orgCredentials = walletProvisioningService.getOrCreateWallet(offering.organizationId());
        var transaction = transactionManager.createAndSubmit(BusinessReferenceType.INVESTMENT_ISSUANCE, investmentId,
                tokenAddress, "mint",
                () -> gateway.mint(tokenAddress, investor.primaryWalletAddress(), units, orgCredentials));
        auditPort.record(new AuditPort.AuditEntry(null, null, "UNITS_MINT_SUBMITTED", "Investment", investmentId,
                null, transaction.getStatus().name(), null, transaction.getTxHash()));
    }

    @Override
    @Transactional
    public void recordDistribution(String offeringId, String distributionRefId, long recordDateEpochSeconds,
            java.math.BigInteger totalAmountRef, byte[] metadataHash) {
        String tokenAddress = resolveTokenAddress(offeringId);
        var transaction = transactionManager.createAndSubmit(BusinessReferenceType.DISTRIBUTION_RECORD, distributionRefId,
                tokenAddress, "recordDistribution", () -> gateway.recordDistribution(tokenAddress, distributionRefId,
                        recordDateEpochSeconds, totalAmountRef, metadataHash));
        auditPort.record(new AuditPort.AuditEntry(null, null, "DISTRIBUTION_RECORD_SUBMITTED", "Distribution",
                distributionRefId, null, transaction.getStatus().name(), null, transaction.getTxHash()));
    }

    @Transactional(readOnly = true)
    public List<ContractDeployment> getDeployments(String offeringId) {
        return contractDeploymentRepository.findByOfferingId(offeringId);
    }

    /** Platform-wide contract deployments, for the admin Tokenization overview. */
    @Transactional(readOnly = true)
    public List<ContractDeployment> listAllDeployments() {
        return contractDeploymentRepository.findAll();
    }

    /**
     * Every organization's on-chain wallet plus its current gas balance — the admin "who needs
     * funding" view (see docs/critical-analysis.md — organization wallets are platform-funded,
     * not the organization's own, so the platform must be able to see which ones are running low).
     * A live {@code eth_getBalance} read per wallet, not cached.
     */
    @Transactional(readOnly = true)
    public List<OrganizationWalletBalance> listOrganizationWallets() {
        return organizationWalletRepository.findAll().stream()
                .map(wallet -> new OrganizationWalletBalance(wallet.getOrganizationId(), wallet.getWalletAddress(),
                        gateway.getBalance(wallet.getWalletAddress())))
                .toList();
    }

    /** Platform-wide blockchain transaction ledger, most recently submitted first. */
    @Transactional(readOnly = true)
    public List<com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction> listTransactions() {
        return blockchainTransactionRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(
                        com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction::getSubmittedAt,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())).reversed())
                .toList();
    }

    /**
     * Resubmits a FAILED blockchain transaction. Only the offering-tokenization "unpause" step is
     * supported today — every other write this unit makes is signed by the platform's own
     * always-funded agent key, so "unpause" (signed by the organization's own wallet) is the only
     * step that plausibly fails for a recoverable reason (the org wallet ran out of gas) rather
     * than a genuine contract-level revert. Reuses the exact submission path {@code
     * deployOfferingToken} uses for the first attempt, so confirmation flows through the normal
     * poller and {@code onTokenizationConfirmed} exactly as it would have the first time.
     */
    @Transactional
    public void retryTransaction(String transactionId) {
        var failed = blockchainTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("BlockchainTransaction", transactionId));
        if (failed.getStatus() != com.rwashift.platform.units.tokenization.domain.model.BlockchainTransactionStatus.FAILED) {
            throw new DomainException("Only FAILED transactions can be retried");
        }
        if (failed.getBusinessReferenceType() != BusinessReferenceType.OFFERING_DEPLOYMENT || !"unpause".equals(failed.getMethod())) {
            throw new DomainException("Retry is only supported for the offering token unpause step");
        }
        String offeringId = failed.getBusinessReferenceId();
        OfferingSnapshot offering = offeringLookupPort.findSnapshot(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        Credentials orgCredentials = walletProvisioningService.getOrCreateWallet(offering.organizationId());
        String tokenAddress = failed.getContractAddress();
        transactionManager.createAndSubmit(BusinessReferenceType.OFFERING_DEPLOYMENT, offeringId, tokenAddress, "unpause",
                () -> gateway.unpauseToken(tokenAddress, orgCredentials));
        auditPort.record(AuditPort.AuditEntry.of(null, offering.organizationId(), "OFFERING_TOKEN_UNPAUSE_RETRIED",
                "Offering", offeringId, failed.getStatus().name(), "SUBMITTED"));
    }

    private String resolveTokenAddress(String offeringId) {
        ContractDeployment deployment = contractDeploymentRepository
                .findByOfferingIdAndContractType(offeringId, ContractType.TOKEN)
                .orElseThrow(() -> new DomainException("Offering has not been tokenized yet"));
        if (deployment.getContractAddress() == null) {
            throw new DomainException("Offering token deployment is not yet confirmed on-chain");
        }
        return deployment.getContractAddress();
    }

    @Override
    public String getPaymentCollectionAddress() {
        return properties.getPaymentCollectionAddress();
    }

    @Override
    public String getUsdcTokenAddress() {
        return properties.getUsdcTokenAddress();
    }

    @Override
    public String getPaymentRouterAddress() {
        return properties.getPaymentRouterAddress();
    }

    /**
     * V1 simplification: a tiny fixed lookup for the demo's supported countries, rather than a
     * full ISO-3166 table. See {@code units.investor} — {@code countryCode} is a two-letter
     * ISO-3166-1 alpha-2 code at rest; this maps it to the numeric code ERC-3643 expects on-chain.
     */
    private static int countryCodeOf(String iso2) {
        return switch (iso2) {
            case "AE" -> 784;
            case "US" -> 840;
            case "GB" -> 826;
            default -> 0;
        };
    }
}
