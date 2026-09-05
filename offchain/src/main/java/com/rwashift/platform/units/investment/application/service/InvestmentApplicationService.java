package com.rwashift.platform.units.investment.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.compliance.application.port.EligibilityLookupPort;
import com.rwashift.platform.units.investment.api.dto.CryptoPaymentInstructionsResponse;
import com.rwashift.platform.units.investment.application.command.InitiateInvestmentCommand;
import com.rwashift.platform.units.investment.application.port.CryptoPaymentConfirmationPort;
import com.rwashift.platform.units.investment.application.port.InvestmentLookupPort;
import com.rwashift.platform.units.investment.application.port.InvestmentSnapshot;
import com.rwashift.platform.units.investment.application.port.PaymentProvider;
import com.rwashift.platform.units.investment.application.port.TokenIssuanceCallbackPort;
import com.rwashift.platform.units.investment.domain.model.Investment;
import com.rwashift.platform.units.investment.domain.model.InvestmentStatus;
import com.rwashift.platform.units.investment.domain.model.PaymentMethod;
import com.rwashift.platform.units.investment.domain.repository.InvestmentRepository;
import com.rwashift.platform.units.investor.application.port.InvestorLookupPort;
import com.rwashift.platform.units.investor.application.port.InvestorSnapshot;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import com.rwashift.platform.units.offering.application.port.OfferingSnapshot;
import com.rwashift.platform.units.tokenization.application.port.PaymentCollectionAddressPort;
import com.rwashift.platform.units.tokenization.application.port.TokenIssuancePort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the required flow: investor requests an amount -> units computed -> offering
 * verified -> eligibility verified -> payment simulated -> token issuance requested -> blockchain
 * confirmation -> SETTLED. Every command here is idempotent by {@code idempotencyKey} where the
 * spec calls for it (investment creation, payment confirmation) — replays return the existing
 * aggregate rather than creating a duplicate or double-charging.
 */
@Service
public class InvestmentApplicationService implements TokenIssuanceCallbackPort, CryptoPaymentConfirmationPort, InvestmentLookupPort {

    private static final Logger log = LoggerFactory.getLogger(InvestmentApplicationService.class);

    private final InvestmentRepository investmentRepository;
    private final OfferingLookupPort offeringLookupPort;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final PaymentProvider paymentProvider;
    private final TokenIssuancePort tokenIssuancePort;
    private final AuditPort auditPort;
    private final InvestorLookupPort investorLookupPort;
    private final PaymentCollectionAddressPort paymentCollectionAddressPort;

    public InvestmentApplicationService(InvestmentRepository investmentRepository, OfferingLookupPort offeringLookupPort,
            EligibilityLookupPort eligibilityLookupPort, PaymentProvider paymentProvider, TokenIssuancePort tokenIssuancePort,
            AuditPort auditPort, InvestorLookupPort investorLookupPort, PaymentCollectionAddressPort paymentCollectionAddressPort) {
        this.investmentRepository = investmentRepository;
        this.offeringLookupPort = offeringLookupPort;
        this.eligibilityLookupPort = eligibilityLookupPort;
        this.paymentProvider = paymentProvider;
        this.tokenIssuancePort = tokenIssuancePort;
        this.auditPort = auditPort;
        this.investorLookupPort = investorLookupPort;
        this.paymentCollectionAddressPort = paymentCollectionAddressPort;
    }

    @Transactional
    public Investment initiateInvestment(InitiateInvestmentCommand command) {
        var existing = investmentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        OfferingSnapshot offering = offeringLookupPort.findSnapshot(command.offeringId())
                .orElseThrow(() -> new NotFoundException("Offering", command.offeringId()));
        if (!offering.openForInvestment()) {
            throw new DomainException("Offering is not open for investment");
        }
        if (command.amount().compareTo(offering.minimumInvestment()) < 0) {
            throw new DomainException("Amount is below the offering's minimum investment");
        }
        BigDecimal[] divideAndRemainder = command.amount().divideAndRemainder(offering.unitPrice());
        if (divideAndRemainder[1].compareTo(BigDecimal.ZERO) != 0) {
            throw new DomainException("Amount must be an exact multiple of the unit price");
        }
        long units = divideAndRemainder[0].setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        if (units <= 0 || units > offering.unitsAvailable()) {
            throw new DomainException("Requested units exceed units available in the offering");
        }

        Investment investment = new Investment(offering.organizationId(), command.investorId(), command.offeringId(),
                command.amount(), offering.currency(), offering.unitPrice(), units, command.idempotencyKey(),
                command.paymentMethod());
        investment.moveToEligibilityPending();

        if (eligibilityLookupPort.evaluate(command.investorId(), command.offeringId())) {
            advanceToPayment(investment);
        }
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "INVESTMENT_INITIATED",
                "Investment", investment.getId(), null, investment.getStatus().name()));
        return investment;
    }

    /** Re-checks eligibility for an investment stuck at ELIGIBILITY_PENDING (e.g. after compliance clears it). */
    @Transactional
    public Investment recheckEligibility(String investmentId) {
        Investment investment = getInvestment(investmentId);
        String previousStatus = investment.getStatus().name();
        if (eligibilityLookupPort.evaluate(investment.getInvestorId(), investment.getOfferingId())) {
            advanceToPayment(investment);
        }
        investment = investmentRepository.save(investment);
        if (!previousStatus.equals(investment.getStatus().name())) {
            auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "ELIGIBILITY_RECHECKED",
                    "Investment", investmentId, previousStatus, investment.getStatus().name()));
        }
        return investment;
    }

    private void advanceToPayment(Investment investment) {
        investment.moveToPaymentPending();
        if (investment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            paymentProvider.initiatePayment(investment.getId(), investment.getAmount(), investment.getCurrency());
        }
        // CRYPTO_WALLET needs no "initiation" side effect — the investor pays by sending USDC
        // directly to the platform's collection address (see getCryptoPaymentInstructions);
        // confirmation is driven by CryptoPaymentWatcher observing that transfer, not by us.
    }

    /** Platform-admin-simulated confirmation for {@code DemoPaymentProvider} (V1 has no real bank webhook). */
    @Transactional
    public Investment confirmPayment(String investmentId) {
        Investment investment = getInvestment(investmentId);
        investment.confirmPayment();
        investment.requestTokenIssuance();
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "PAYMENT_CONFIRMED",
                "Investment", investmentId, "PAYMENT_PENDING", investment.getStatus().name()));
        tokenIssuancePort.requestUnitIssuance(investment.getId(), investment.getOfferingId(),
                investment.getInvestorId(), investment.getUnits());
        return investment;
    }

    /** Investor's own self-service withdrawal — see {@code InvestmentStatus}'s Javadoc for why this only works pre-payment-confirmation. */
    @Transactional
    public Investment cancelInvestment(String investmentId) {
        Investment investment = getInvestment(investmentId);
        String previousStatus = investment.getStatus().name();
        investment.cancel();
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "INVESTMENT_CANCELLED",
                "Investment", investmentId, previousStatus, investment.getStatus().name()));
        return investment;
    }

    @Transactional(readOnly = true)
    public CryptoPaymentInstructionsResponse getCryptoPaymentInstructions(String investmentId) {
        Investment investment = getInvestment(investmentId);
        if (investment.getPaymentMethod() != PaymentMethod.CRYPTO_WALLET) {
            throw new DomainException("This investment is not using crypto wallet payment");
        }
        InvestorSnapshot investor = investorLookupPort.findSnapshot(investment.getInvestorId())
                .orElseThrow(() -> new NotFoundException("Investor", investment.getInvestorId()));
        return new CryptoPaymentInstructionsResponse(investment.getId(), paymentCollectionAddressPort.getPaymentRouterAddress(),
                paymentCollectionAddressPort.getUsdcTokenAddress(), investment.getAmount(), investor.primaryWalletAddress());
    }

    /**
     * The investment id comes straight from the on-chain {@code PaymentReceived} event — no
     * matching/guessing involved, see {@code RwaShiftPaymentRouter}'s and
     * {@code CryptoPaymentConfirmationPort}'s Javadoc. This method's job is purely to validate
     * that the payment actually applies: the investment must exist, be genuinely awaiting a
     * crypto payment, and the amount paid must match what's owed — a router event referencing an
     * unknown, already-settled, or wrong-amount investment is logged and ignored rather than
     * trusted blindly.
     */
    @Override
    @Transactional
    public void onCryptoPaymentDetected(String investmentId, String txHash, String payerWalletAddress, BigDecimal amount) {
        Investment investment = investmentRepository.findById(investmentId).orElse(null);
        if (investment == null) {
            log.warn("Crypto payment (tx {}) references unknown investment {} — ignoring", txHash, investmentId);
            return;
        }
        if (investment.getStatus() != InvestmentStatus.PAYMENT_PENDING || investment.getPaymentMethod() != PaymentMethod.CRYPTO_WALLET) {
            log.warn("Crypto payment (tx {}) received for investment {} which is not awaiting a crypto payment (status={}, method={}) — ignoring",
                    txHash, investmentId, investment.getStatus(), investment.getPaymentMethod());
            return;
        }
        if (investment.getAmount().compareTo(amount) != 0) {
            log.warn("Crypto payment (tx {}) for investment {} paid {} but {} was owed — ignoring, not confirming a partial/incorrect payment",
                    txHash, investmentId, amount, investment.getAmount());
            return;
        }

        investment.confirmCryptoPayment(txHash, payerWalletAddress);
        investment.requestTokenIssuance();
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "PAYMENT_CONFIRMED",
                "Investment", investment.getId(), "PAYMENT_PENDING", investment.getStatus().name()));
        tokenIssuancePort.requestUnitIssuance(investment.getId(), investment.getOfferingId(),
                investment.getInvestorId(), investment.getUnits());
    }

    @Transactional
    public Investment failPayment(String investmentId, String reason) {
        Investment investment = getInvestment(investmentId);
        String previousStatus = investment.getStatus().name();
        investment.failPayment(reason);
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "PAYMENT_FAILED",
                "Investment", investmentId, previousStatus, investment.getStatus().name()));
        return investment;
    }

    @Override
    @Transactional
    public void onTokenIssuanceConfirmed(String investmentId) {
        Investment investment = getInvestment(investmentId);
        String previousStatus = investment.getStatus().name();
        investment.settleTokenIssuance();
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "TOKEN_ISSUANCE_CONFIRMED",
                "Investment", investmentId, previousStatus, investment.getStatus().name()));
        offeringLookupPort.recordUnitsIssued(investment.getOfferingId(), investment.getUnits());
    }

    @Override
    @Transactional
    public void onTokenIssuanceFailed(String investmentId, String reason) {
        Investment investment = getInvestment(investmentId);
        String previousStatus = investment.getStatus().name();
        investment.failTokenIssuance(reason);
        investment = investmentRepository.save(investment);
        auditPort.record(AuditPort.AuditEntry.of(null, investment.getOrganizationId(), "TOKEN_ISSUANCE_FAILED",
                "Investment", investmentId, previousStatus, investment.getStatus().name()));
    }

    @Transactional(readOnly = true)
    public Investment getInvestment(String investmentId) {
        return investmentRepository.findById(investmentId)
                .orElseThrow(() -> new NotFoundException("Investment", investmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<InvestmentSnapshot> findSnapshot(String investmentId) {
        return investmentRepository.findById(investmentId)
                .map(i -> new InvestmentSnapshot(i.getId(), i.getOfferingId(), i.getUnits()));
    }

    @Transactional(readOnly = true)
    public List<Investment> listForInvestor(String investorId) {
        return investmentRepository.findByInvestorId(investorId);
    }

    /** {@code organizationId == null} means the caller is a platform admin (no org of their own) — return every investment platform-wide. */
    @Transactional(readOnly = true)
    public List<Investment> listForOrganization(String organizationId) {
        return organizationId == null ? investmentRepository.findAll() : investmentRepository.findByOrganizationId(organizationId);
    }
}
