package com.rwashift.platform.units.kyc.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * V1's KYC is simulated/manual: a compliance officer reviews and verifies/rejects through the
 * admin API. {@link com.rwashift.platform.units.kyc.application.port.KycProvider} is the seam a
 * production identity-verification vendor would plug into later without this aggregate or its
 * state machine changing.
 */
@Entity
@Table(name = "kyc_case")
public class KycCase {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "investor_id", nullable = false, unique = true, length = 26)
    private String investorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private KycStatus status = KycStatus.NOT_STARTED;

    @Column(name = "provider_reference", length = 200)
    private String providerReference;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 26)
    private String reviewedBy;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected KycCase() {
    }

    public KycCase(String investorId) {
        this.investorId = investorId;
    }

    public void submit(String providerReference) {
        transitionTo(KycStatus.SUBMITTED);
        this.providerReference = providerReference;
        this.submittedAt = Instant.now();
        this.rejectionReason = null;
    }

    public void startReview() {
        transitionTo(KycStatus.UNDER_REVIEW);
    }

    public void verify(String reviewerId) {
        transitionTo(KycStatus.VERIFIED);
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    public void reject(String reviewerId, String reason) {
        transitionTo(KycStatus.REJECTED);
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.rejectionReason = reason;
    }

    private void transitionTo(KycStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("KycCase", status.name(), target.name());
        }
        this.status = target;
    }

    public String getId() {
        return id;
    }

    public String getInvestorId() {
        return investorId;
    }

    public KycStatus getStatus() {
        return status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
