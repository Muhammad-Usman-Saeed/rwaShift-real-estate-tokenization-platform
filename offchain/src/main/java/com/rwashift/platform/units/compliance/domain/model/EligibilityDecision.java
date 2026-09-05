package com.rwashift.platform.units.compliance.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The off-chain eligibility decision for one (investor, offering) pair — the sole gate before
 * Compliance orchestrates on-chain identity registration through Tokenization. Rules that
 * produced the decision are recorded as opaque reason codes (see
 * {@code units.compliance.domain.policy.EligibilityRule}); nothing jurisdiction-specific is
 * hardcoded here — see ADR notes in the README on configurable compliance rules.
 */
@Entity
@Table(name = "eligibility_decision",
        uniqueConstraints = @UniqueConstraint(columnNames = {"investor_id", "offering_id"}))
public class EligibilityDecision {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "investor_id", nullable = false, length = 26)
    private String investorId;

    @Column(name = "offering_id", nullable = false, length = 26)
    private String offeringId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EligibilityStatus status;

    @ElementCollection
    @CollectionTable(name = "eligibility_decision_reason", joinColumns = @JoinColumn(name = "decision_id"))
    @Column(name = "reason_code", length = 100)
    private List<String> reasonCodes = new ArrayList<>();

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    @Column(name = "identity_registered", nullable = false)
    private boolean identityRegistered = false;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EligibilityDecision() {
    }

    public EligibilityDecision(String investorId, String offeringId) {
        this.investorId = investorId;
        this.offeringId = offeringId;
        this.status = EligibilityStatus.PENDING;
    }

    public void recordEvaluation(EligibilityStatus status, List<String> reasonCodes) {
        this.status = status;
        this.reasonCodes = new ArrayList<>(reasonCodes);
        this.evaluatedAt = Instant.now();
    }

    public void markIdentityRegistered() {
        this.identityRegistered = true;
    }

    public boolean becameEligible(EligibilityStatus previousStatus) {
        return previousStatus != EligibilityStatus.ELIGIBLE && this.status == EligibilityStatus.ELIGIBLE;
    }

    public String getId() {
        return id;
    }

    public String getInvestorId() {
        return investorId;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public EligibilityStatus getStatus() {
        return status;
    }

    public List<String> getReasonCodes() {
        return List.copyOf(reasonCodes);
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public boolean isIdentityRegistered() {
        return identityRegistered;
    }
}
