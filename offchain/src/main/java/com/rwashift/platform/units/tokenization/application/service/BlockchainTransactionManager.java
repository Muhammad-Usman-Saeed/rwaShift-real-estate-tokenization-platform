package com.rwashift.platform.units.tokenization.application.service;

import com.rwashift.platform.units.compliance.application.port.IdentityRegistrationConfirmationPort;
import com.rwashift.platform.units.investment.application.port.TokenIssuanceCallbackPort;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import com.rwashift.platform.units.offering.application.port.OfferingSnapshot;
import com.rwashift.platform.units.offering.application.port.OfferingTokenizationCallbackPort;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransactionStatus;
import com.rwashift.platform.units.tokenization.domain.model.BusinessReferenceType;
import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;
import com.rwashift.platform.units.tokenization.domain.model.ContractType;
import com.rwashift.platform.units.tokenization.domain.repository.BlockchainTransactionRepository;
import com.rwashift.platform.units.tokenization.domain.repository.ContractDeploymentRepository;
import com.rwashift.platform.units.tokenization.infrastructure.configuration.BlockchainProperties;
import com.rwashift.platform.units.tokenization.infrastructure.integration.Web3jContractGateway;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Owns the full lifecycle of every blockchain write this platform makes:
 * {@code CREATED -> SUBMITTED -> PENDING -> CONFIRMED/FAILED} (ADR-010). Submission
 * ({@link #createAndSubmit}) is synchronous only up to "the node accepted the transaction into
 * its mempool" — nothing here ever waits for a receipt inline. A single {@link Scheduled}
 * poller checks every outstanding transaction's receipt, applies the configured confirmation
 * depth, and dispatches the appropriate cross-unit callback once (and only once) a transaction
 * is truly confirmed. Designed to tolerate restarts (state lives in the DB, not memory),
 * RPC hiccups (each poll iteration is independent and retries next tick), and reverted
 * transactions (explicitly modeled as {@code FAILED}, never silently ignored).
 */
@Service
class BlockchainTransactionManager {

    private static final Logger log = LoggerFactory.getLogger(BlockchainTransactionManager.class);

    private final BlockchainTransactionRepository transactionRepository;
    private final ContractDeploymentRepository contractDeploymentRepository;
    private final Web3jContractGateway gateway;
    private final BlockchainProperties properties;
    private final OrganizationWalletProvisioningService walletProvisioningService;
    private final OfferingLookupPort offeringLookupPort;
    private final TokenIssuanceCallbackPort tokenIssuanceCallbackPort;
    private final OfferingTokenizationCallbackPort offeringTokenizationCallbackPort;
    private final IdentityRegistrationConfirmationPort identityRegistrationConfirmationPort;

    // @Lazy on these three: Investment (via TokenIssuanceCallbackPort), Offering (via
    // OfferingTokenizationCallbackPort), and Compliance (via IdentityRegistrationConfirmationPort)
    // all sit downstream of Tokenization in the bean graph, but Compliance -> Tokenization ->
    // (this manager) -> {Investment, Compliance} forms a real cycle (Investment needs
    // Compliance's EligibilityLookupPort to gate investment creation; Compliance itself calls
    // back here once identity registration confirms). Lazy proxies here defer resolving that
    // edge until first actual use (i.e. once a transaction confirms), by which point the whole
    // context is up — breaking the cycle without weakening any actual runtime behavior.
    // OfferingLookupPort is a plain read port with no such cycle (TokenizationApplicationService
    // already injects it non-lazily), so it's wired directly.
    BlockchainTransactionManager(BlockchainTransactionRepository transactionRepository,
            ContractDeploymentRepository contractDeploymentRepository, Web3jContractGateway gateway,
            BlockchainProperties properties, OrganizationWalletProvisioningService walletProvisioningService,
            OfferingLookupPort offeringLookupPort, @Lazy TokenIssuanceCallbackPort tokenIssuanceCallbackPort,
            @Lazy OfferingTokenizationCallbackPort offeringTokenizationCallbackPort,
            @Lazy IdentityRegistrationConfirmationPort identityRegistrationConfirmationPort) {
        this.transactionRepository = transactionRepository;
        this.contractDeploymentRepository = contractDeploymentRepository;
        this.gateway = gateway;
        this.properties = properties;
        this.walletProvisioningService = walletProvisioningService;
        this.offeringLookupPort = offeringLookupPort;
        this.tokenIssuanceCallbackPort = tokenIssuanceCallbackPort;
        this.offeringTokenizationCallbackPort = offeringTokenizationCallbackPort;
        this.identityRegistrationConfirmationPort = identityRegistrationConfirmationPort;
    }

    @Transactional
    BlockchainTransaction createAndSubmit(BusinessReferenceType type, String businessReferenceId,
            String contractAddress, String method, java.util.function.Supplier<String> submitter) {
        BlockchainTransaction transaction = new BlockchainTransaction(
                type, businessReferenceId, properties.getNetwork(), properties.getChainId(), contractAddress, method);
        transaction = transactionRepository.save(transaction);
        try {
            String txHash = submitter.get();
            transaction.markSubmitted(txHash);
        } catch (Exception e) {
            log.error("Failed to submit {} for {}:{}", method, type, businessReferenceId, e);
            transaction.markFailed(e.getMessage());
            transaction = transactionRepository.save(transaction);
            dispatchFailure(transaction);
            return transaction;
        }
        return transactionRepository.save(transaction);
    }

    @Scheduled(fixedDelayString = "${rwashift.blockchain.poll-interval-ms:4000}")
    @Transactional
    void pollOutstandingTransactions() {
        List<BlockchainTransaction> outstanding = transactionRepository.findByStatusIn(
                List.of(BlockchainTransactionStatus.SUBMITTED, BlockchainTransactionStatus.PENDING));
        for (BlockchainTransaction transaction : outstanding) {
            try {
                pollOne(transaction);
            } catch (Exception e) {
                log.warn("Error polling blockchain transaction {}", transaction.getId(), e);
            }
        }
    }

    private void pollOne(BlockchainTransaction transaction) {
        TransactionReceipt receipt = gateway.fetchReceipt(transaction.getTxHash());
        if (receipt == null) {
            if (transaction.getStatus() == BlockchainTransactionStatus.SUBMITTED) {
                transaction.markPending();
                transactionRepository.save(transaction);
            }
            return;
        }
        if (!receipt.isStatusOK()) {
            transaction.markFailed("Transaction reverted");
            transactionRepository.save(transaction);
            dispatchFailure(transaction);
            return;
        }
        long receiptBlock = receipt.getBlockNumber().longValueExact();
        long currentBlock = gateway.currentBlockNumber();
        long confirmations = currentBlock - receiptBlock + 1;
        if (confirmations < properties.getConfirmationsRequired()) {
            if (transaction.getStatus() == BlockchainTransactionStatus.SUBMITTED) {
                transaction.markPending();
                transactionRepository.save(transaction);
            }
            return;
        }
        transaction.markConfirmed(receiptBlock);
        transactionRepository.save(transaction);
        dispatchConfirmation(transaction);
    }

    private void dispatchConfirmation(BlockchainTransaction transaction) {
        switch (transaction.getBusinessReferenceType()) {
            case INVESTMENT_ISSUANCE -> tokenIssuanceCallbackPort.onTokenIssuanceConfirmed(transaction.getBusinessReferenceId());
            case OFFERING_DEPLOYMENT -> onOfferingDeploymentTransactionConfirmed(transaction);
            case IDENTITY_REGISTRATION -> onIdentityRegistrationTransactionConfirmed(transaction);
            case DISTRIBUTION_RECORD -> log.info("{} confirmed for {}", transaction.getMethod(), transaction.getBusinessReferenceId());
        }
    }

    /** {@code businessReferenceId} is {@code "<investorId>:<offeringId>"} — see TokenizationApplicationService#registerInvestorIdentity. */
    private void onIdentityRegistrationTransactionConfirmed(BlockchainTransaction transaction) {
        String[] parts = transaction.getBusinessReferenceId().split(":", 2);
        if (parts.length != 2) {
            log.warn("Unexpected IDENTITY_REGISTRATION business reference id format: {}", transaction.getBusinessReferenceId());
            return;
        }
        identityRegistrationConfirmationPort.onIdentityRegistrationConfirmed(parts[0], parts[1]);
    }

    private void dispatchFailure(BlockchainTransaction transaction) {
        if (transaction.getBusinessReferenceType() == BusinessReferenceType.INVESTMENT_ISSUANCE) {
            tokenIssuanceCallbackPort.onTokenIssuanceFailed(transaction.getBusinessReferenceId(), transaction.getFailureReason());
        } else {
            log.error("{} failed for {}:{} - {}", transaction.getMethod(), transaction.getBusinessReferenceType(),
                    transaction.getBusinessReferenceId(), transaction.getFailureReason());
        }
        // Without this, a reverted "createOffering" leaves its ContractDeployment row stuck
        // PENDING forever — the issuer's Tokenize screen would spin on "Waiting for
        // confirmation..." indefinitely instead of ever surfacing the failure.
        if (transaction.getBusinessReferenceType() == BusinessReferenceType.OFFERING_DEPLOYMENT
                && "createOffering".equals(transaction.getMethod())) {
            contractDeploymentRepository.findByOfferingIdAndContractType(transaction.getBusinessReferenceId(), ContractType.TOKEN)
                    .ifPresent(deployment -> {
                        deployment.fail();
                        contractDeploymentRepository.save(deployment);
                    });
        }
    }

    private void onOfferingDeploymentTransactionConfirmed(BlockchainTransaction transaction) {
        String offeringId = transaction.getBusinessReferenceId();
        if ("createOffering".equals(transaction.getMethod())) {
            String tokenAddress = gateway.tokenForOffering(offeringId);
            ContractDeployment deployment = contractDeploymentRepository
                    .findByOfferingIdAndContractType(offeringId, ContractType.TOKEN)
                    .orElseThrow(() -> new IllegalStateException("No ContractDeployment record for offering " + offeringId));
            deployment.confirm(tokenAddress, transaction.getBlockNumber());
            contractDeploymentRepository.save(deployment);

            OfferingSnapshot offering = offeringLookupPort.findSnapshot(offeringId)
                    .orElseThrow(() -> new IllegalStateException("No offering record for " + offeringId));
            var orgCredentials = walletProvisioningService.getOrCreateWallet(offering.organizationId());
            createAndSubmit(BusinessReferenceType.OFFERING_DEPLOYMENT, offeringId, tokenAddress, "unpause",
                    () -> gateway.unpauseToken(tokenAddress, orgCredentials));
        } else if ("unpause".equals(transaction.getMethod())) {
            offeringTokenizationCallbackPort.onTokenizationConfirmed(offeringId);
        }
    }
}
