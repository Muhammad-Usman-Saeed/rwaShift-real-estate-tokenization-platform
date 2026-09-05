package com.rwashift.platform.units.investor.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A wallet address an investor has linked. V1 only ever treats the wallet flagged
 * {@link #primary} as the investment wallet (mirrored on {@link Investor#getPrimaryWalletAddress()});
 * the table still records every linked wallet so a future multi-wallet flow needs no migration.
 */
@Entity
@Table(name = "investor_linked_wallet")
public class LinkedWallet {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "investor_id", nullable = false, length = 26)
    private String investorId;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt = Instant.now();

    protected LinkedWallet() {
    }

    public LinkedWallet(String investorId, String walletAddress, boolean primary) {
        this.investorId = investorId;
        this.walletAddress = walletAddress;
        this.primary = primary;
    }

    public String getId() {
        return id;
    }

    public String getInvestorId() {
        return investorId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
