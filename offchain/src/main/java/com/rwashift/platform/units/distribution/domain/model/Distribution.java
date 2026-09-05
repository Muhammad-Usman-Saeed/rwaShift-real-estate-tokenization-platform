package com.rwashift.platform.units.distribution.domain.model;

import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import com.rwashift.platform.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One distribution run against one offering (e.g. "$100,000 rental income distribution").
 * Entitlements ({@link com.rwashift.platform.units.distribution.domain.model.DistributionEntitlement})
 * are computed from an Ownership snapshot taken at {@link #calculate}-time — V1 treats "current
 * projection at calculation time" as the record-date snapshot (a real record-date/ex-date
 * snapshot mechanism is a natural extension, not implemented here). Settlement is simulated.
 */
@Entity
@Table(name = "distribution")
public class Distribution extends TenantScopedEntity {

    @Column(name = "offering_id", nullable = false, length = 26)
    private String offeringId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "record_date", nullable = false)
    private Instant recordDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DistributionStatus status = DistributionStatus.DRAFT;

    protected Distribution() {
    }

    public Distribution(String organizationId, String offeringId, BigDecimal totalAmount, String currency, Instant recordDate) {
        super(organizationId);
        this.offeringId = offeringId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.recordDate = recordDate;
    }

    public void markCalculated() {
        transitionTo(DistributionStatus.CALCULATED);
    }

    public void approve() {
        transitionTo(DistributionStatus.APPROVED);
    }

    public void startProcessing() {
        transitionTo(DistributionStatus.PROCESSING);
    }

    public void complete() {
        transitionTo(DistributionStatus.COMPLETED);
    }

    private void transitionTo(DistributionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Distribution", status.name(), target.name());
        }
        this.status = target;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getRecordDate() {
        return recordDate;
    }

    public DistributionStatus getStatus() {
        return status;
    }
}
