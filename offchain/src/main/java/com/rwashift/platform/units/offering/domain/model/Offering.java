package com.rwashift.platform.units.offering.domain.model;

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
 * The investable offer against a {@code LegalStructure} (e.g. "$2,000,000 raise, 20,000 units
 * at $100"). Distinct from the {@code Asset} (the property) and from the eventual ERC-3643
 * token (see {@code units.tokenization}) — this aggregate owns only the offering's business
 * terms and its approval/tokenization/fundraising lifecycle. State is only ever changed through
 * the explicit transition methods below; there is no public setter for {@link #status}.
 */
@Entity
@Table(name = "offering")
public class Offering extends TenantScopedEntity {

    @Column(name = "asset_id", nullable = false, length = 26)
    private String assetId;

    @Column(name = "legal_structure_id", nullable = false, length = 26)
    private String legalStructureId;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "target_raise", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetRaise;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_units", nullable = false)
    private long totalUnits;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "minimum_investment", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumInvestment;

    @Column(name = "offered_interest_percentage", nullable = false, precision = 7, scale = 4)
    private BigDecimal offeredInterestPercentage;

    @Column(name = "opening_date")
    private Instant openingDate;

    @Column(name = "closing_date")
    private Instant closingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OfferingStatus status = OfferingStatus.DRAFT;

    /** Tracks units already issued via Investment/Tokenization; never mutated directly by API callers. */
    @Column(name = "units_issued", nullable = false)
    private long unitsIssued = 0;

    protected Offering() {
    }

    public Offering(String organizationId, String assetId, String legalStructureId, String name,
            BigDecimal targetRaise, String currency, long totalUnits, BigDecimal unitPrice,
            BigDecimal minimumInvestment, BigDecimal offeredInterestPercentage, Instant openingDate, Instant closingDate) {
        super(organizationId);
        this.assetId = assetId;
        this.legalStructureId = legalStructureId;
        this.name = name;
        this.targetRaise = targetRaise;
        this.currency = currency;
        this.totalUnits = totalUnits;
        this.unitPrice = unitPrice;
        this.minimumInvestment = minimumInvestment;
        this.offeredInterestPercentage = offeredInterestPercentage;
        this.openingDate = openingDate;
        this.closingDate = closingDate;
    }

    public void update(String name, BigDecimal targetRaise, String currency, long totalUnits, BigDecimal unitPrice,
            BigDecimal minimumInvestment, BigDecimal offeredInterestPercentage, Instant openingDate, Instant closingDate) {
        this.name = name;
        this.targetRaise = targetRaise;
        this.currency = currency;
        this.totalUnits = totalUnits;
        this.unitPrice = unitPrice;
        this.minimumInvestment = minimumInvestment;
        this.offeredInterestPercentage = offeredInterestPercentage;
        this.openingDate = openingDate;
        this.closingDate = closingDate;
    }

    public void submitForReview() {
        transitionTo(OfferingStatus.UNDER_REVIEW);
    }

    public void approve() {
        transitionTo(OfferingStatus.APPROVED);
    }

    public void reject() {
        transitionTo(OfferingStatus.REJECTED);
    }

    public void beginTokenization() {
        transitionTo(OfferingStatus.TOKENIZING);
    }

    public void openForInvestment() {
        transitionTo(OfferingStatus.OPEN);
    }

    public void markFunded() {
        transitionTo(OfferingStatus.FUNDED);
    }

    public void close() {
        transitionTo(OfferingStatus.CLOSED);
    }

    /** Called by Investment (via its port to Offering, or Tokenization confirmation) once units settle on-chain. */
    public void recordUnitsIssued(long units) {
        long newTotal = this.unitsIssued + units;
        if (newTotal > totalUnits) {
            throw new com.rwashift.platform.shared.domain.DomainException(
                    "Issuing %d units would exceed offering capacity of %d".formatted(units, totalUnits));
        }
        this.unitsIssued = newTotal;
        if (this.unitsIssued == totalUnits && status == OfferingStatus.OPEN) {
            markFunded();
        }
    }

    private void transitionTo(OfferingStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Offering", status.name(), target.name());
        }
        this.status = target;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getLegalStructureId() {
        return legalStructureId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetRaise() {
        return targetRaise;
    }

    public String getCurrency() {
        return currency;
    }

    public long getTotalUnits() {
        return totalUnits;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getMinimumInvestment() {
        return minimumInvestment;
    }

    public BigDecimal getOfferedInterestPercentage() {
        return offeredInterestPercentage;
    }

    public Instant getOpeningDate() {
        return openingDate;
    }

    public Instant getClosingDate() {
        return closingDate;
    }

    public OfferingStatus getStatus() {
        return status;
    }

    public long getUnitsIssued() {
        return unitsIssued;
    }

    public long getUnitsAvailable() {
        return totalUnits - unitsIssued;
    }
}
