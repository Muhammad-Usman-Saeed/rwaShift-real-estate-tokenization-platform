package com.rwashift.platform.units.ownership.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigInteger;
import java.time.Instant;

/**
 * THIS IS A PROJECTION, NOT A SOURCE OF TRUTH. Actual token ownership is determined solely by
 * the ERC-3643 token contract on-chain (see the separate `onchain/` project); this table exists
 * only so balances can be queried without an RPC round trip per request, kept in sync by
 * {@code Tokenization}'s event indexer replaying {@code Transfer} events. No API in this unit
 * ever allows directly writing {@link #units} — the only mutation path is
 * {@code OwnershipProjectionPort.applyTransfer}, called exclusively by the indexer.
 */
@Entity
@Table(name = "ownership_record", uniqueConstraints = @UniqueConstraint(columnNames = {"token_address", "wallet_address"}))
public class OwnershipRecord {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "investor_id", length = 26)
    private String investorId;

    @Column(name = "offering_id", nullable = false, length = 26)
    private String offeringId;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "token_address", nullable = false, length = 42)
    private String tokenAddress;

    @Column(name = "units", nullable = false, precision = 38, scale = 0)
    private BigInteger units = BigInteger.ZERO;

    @Column(name = "last_synced_block", nullable = false)
    private long lastSyncedBlock;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected OwnershipRecord() {
    }

    public OwnershipRecord(String investorId, String offeringId, String walletAddress, String tokenAddress) {
        this.investorId = investorId;
        this.offeringId = offeringId;
        this.walletAddress = walletAddress;
        this.tokenAddress = tokenAddress;
    }

    public void applyDelta(BigInteger delta, long blockNumber) {
        this.units = this.units.add(delta);
        this.lastSyncedBlock = blockNumber;
        this.updatedAt = Instant.now();
    }

    public void linkInvestor(String investorId) {
        this.investorId = investorId;
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

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public BigInteger getUnits() {
        return units;
    }

    public long getLastSyncedBlock() {
        return lastSyncedBlock;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
