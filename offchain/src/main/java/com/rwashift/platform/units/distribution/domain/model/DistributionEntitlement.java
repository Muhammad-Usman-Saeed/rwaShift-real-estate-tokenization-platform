package com.rwashift.platform.units.distribution.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;

/** One investor's pro-rata share of a {@link Distribution}, computed at calculation time. */
@Entity
@Table(name = "distribution_entitlement")
public class DistributionEntitlement {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "distribution_id", nullable = false, length = 26)
    private String distributionId;

    @Column(name = "investor_id", length = 26)
    private String investorId;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "units_at_record_date", nullable = false, precision = 38, scale = 0)
    private BigInteger unitsAtRecordDate;

    @Column(name = "entitlement_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal entitlementAmount;

    protected DistributionEntitlement() {
    }

    public DistributionEntitlement(String distributionId, String investorId, String walletAddress,
            BigInteger unitsAtRecordDate, BigDecimal entitlementAmount) {
        this.distributionId = distributionId;
        this.investorId = investorId;
        this.walletAddress = walletAddress;
        this.unitsAtRecordDate = unitsAtRecordDate;
        this.entitlementAmount = entitlementAmount;
    }

    public String getId() {
        return id;
    }

    public String getDistributionId() {
        return distributionId;
    }

    public String getInvestorId() {
        return investorId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public BigInteger getUnitsAtRecordDate() {
        return unitsAtRecordDate;
    }

    public BigDecimal getEntitlementAmount() {
        return entitlementAmount;
    }
}
