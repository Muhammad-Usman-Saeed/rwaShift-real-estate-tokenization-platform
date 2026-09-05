package com.rwashift.platform.units.investment.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * The real financial aggregate: one investor's commitment against one offering. Money fields
 * are {@link BigDecimal} throughout — never {@code double}/{@code float} (see ADR notes on
 * financial precision). {@code organizationId} is denormalized from the Offering at creation
 * time purely so issuer-side reporting/audit can scope by tenant without joining across units;
 * Investment itself is not "owned" by the issuer org the way Asset/Offering are (it is the
 * investor's record too).
 */
@Entity
@Table(name = "investment",
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class Investment extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "organization_id", nullable = false, length = 26, updatable = false)
    private String organizationId;

    @Column(name = "investor_id", nullable = false, length = 26, updatable = false)
    private String investorId;

    @Column(name = "offering_id", nullable = false, length = 26, updatable = false)
    private String offeringId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "units", nullable = false)
    private long units;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvestmentStatus status = InvestmentStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_issuance_status", nullable = false, length = 32)
    private TokenIssuanceStatus tokenIssuanceStatus = TokenIssuanceStatus.NOT_STARTED;

    @Column(name = "idempotency_key", nullable = false, length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    /** Set once a matching on-chain transfer is detected — see {@code CryptoPaymentWatcher}. Null for BANK_TRANSFER. */
    @Column(name = "payment_tx_hash", length = 66)
    private String paymentTxHash;

    /** The wallet that actually sent the crypto payment, for reconciliation against the investor's registered wallet. */
    @Column(name = "payer_wallet_address", length = 42)
    private String payerWalletAddress;

    protected Investment() {
    }

    public Investment(String organizationId, String investorId, String offeringId, BigDecimal amount,
            String currency, BigDecimal unitPrice, long units, String idempotencyKey) {
        this(organizationId, investorId, offeringId, amount, currency, unitPrice, units, idempotencyKey,
                PaymentMethod.BANK_TRANSFER);
    }

    public Investment(String organizationId, String investorId, String offeringId, BigDecimal amount,
            String currency, BigDecimal unitPrice, long units, String idempotencyKey, PaymentMethod paymentMethod) {
        this.organizationId = organizationId;
        this.investorId = investorId;
        this.offeringId = offeringId;
        this.amount = amount;
        this.currency = currency;
        this.unitPrice = unitPrice;
        this.units = units;
        this.idempotencyKey = idempotencyKey;
        this.paymentMethod = paymentMethod;
    }

    public void moveToEligibilityPending() {
        transitionTo(InvestmentStatus.ELIGIBILITY_PENDING);
    }

    public void moveToPaymentPending() {
        transitionTo(InvestmentStatus.PAYMENT_PENDING);
    }

    public void rejectOnEligibility() {
        transitionTo(InvestmentStatus.ELIGIBILITY_REJECTED);
    }

    /** Investor's own self-service withdrawal — only valid before payment is confirmed; see {@link InvestmentStatus}'s Javadoc. */
    public void cancel() {
        transitionTo(InvestmentStatus.CANCELLED);
    }

    public void confirmPayment() {
        this.paymentStatus = PaymentStatus.CONFIRMED;
        transitionTo(InvestmentStatus.CONFIRMED);
    }

    /** Called by {@code CryptoPaymentWatcher} once it matches an on-chain transfer to this investment. */
    public void confirmCryptoPayment(String txHash, String payerWalletAddress) {
        this.paymentTxHash = txHash;
        this.payerWalletAddress = payerWalletAddress;
        confirmPayment();
    }

    public void failPayment(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureReason = reason;
        transitionTo(InvestmentStatus.PAYMENT_FAILED);
    }

    public void requestTokenIssuance() {
        this.tokenIssuanceStatus = TokenIssuanceStatus.PENDING;
        transitionTo(InvestmentStatus.TOKEN_ISSUANCE_PENDING);
    }

    public void settleTokenIssuance() {
        this.tokenIssuanceStatus = TokenIssuanceStatus.CONFIRMED;
        transitionTo(InvestmentStatus.SETTLED);
    }

    public void failTokenIssuance(String reason) {
        this.tokenIssuanceStatus = TokenIssuanceStatus.FAILED;
        this.failureReason = reason;
        transitionTo(InvestmentStatus.TOKEN_ISSUANCE_FAILED);
    }

    private void transitionTo(InvestmentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Investment", status.name(), target.name());
        }
        this.status = target;
    }

    public String getId() {
        return id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getInvestorId() {
        return investorId;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public long getUnits() {
        return units;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public TokenIssuanceStatus getTokenIssuanceStatus() {
        return tokenIssuanceStatus;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentTxHash() {
        return paymentTxHash;
    }

    public String getPayerWalletAddress() {
        return payerWalletAddress;
    }
}
