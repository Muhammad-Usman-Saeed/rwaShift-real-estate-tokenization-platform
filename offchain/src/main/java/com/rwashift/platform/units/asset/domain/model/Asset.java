package com.rwashift.platform.units.asset.domain.model;

import com.rwashift.platform.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * The physical/economic real-world asset (e.g. "Dubai Business Tower"). Deliberately NOT the
 * legal owner and NOT the investable instrument — see {@code units.legalstructure} for the SPV
 * that legally owns this asset and {@code units.offering} for what is actually sold to
 * investors. An Asset can exist, be valued, and be verified before any legal structure or
 * offering is created against it.
 */
@Entity
@Table(name = "asset")
public class Asset extends TenantScopedEntity {

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private AssetType type;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Column(name = "valuation", nullable = false, precision = 19, scale = 4)
    private BigDecimal valuation;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AssetStatus status = AssetStatus.DRAFT;

    protected Asset() {
    }

    public Asset(String organizationId, String name, AssetType type, String description, String location,
            BigDecimal valuation, String currency) {
        super(organizationId);
        this.name = name;
        this.type = type;
        this.description = description;
        this.location = location;
        this.valuation = valuation;
        this.currency = currency;
    }

    public void update(String name, String description, String location, BigDecimal valuation, String currency) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.valuation = valuation;
        this.currency = currency;
    }

    public void markUnderVerification() {
        this.status = AssetStatus.UNDER_VERIFICATION;
    }

    public void markVerified() {
        this.status = AssetStatus.VERIFIED;
    }

    public void archive() {
        this.status = AssetStatus.ARCHIVED;
    }

    public String getName() {
        return name;
    }

    public AssetType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public BigDecimal getValuation() {
        return valuation;
    }

    public String getCurrency() {
        return currency;
    }

    public AssetStatus getStatus() {
        return status;
    }
}
