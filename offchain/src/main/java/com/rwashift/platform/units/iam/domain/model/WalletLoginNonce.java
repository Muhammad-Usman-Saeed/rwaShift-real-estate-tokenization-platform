package com.rwashift.platform.units.iam.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A single-use, short-lived challenge issued to one wallet address so its holder can prove
 * ownership by signing a message containing it (see {@code WalletAuthenticationService} for
 * issuance, {@code WalletAuthenticationProvider} for consumption). One row per address — a new
 * sign-in attempt simply overwrites the previous nonce, invalidating it.
 */
@Entity
@Table(name = "wallet_login_nonce")
public class WalletLoginNonce {

    @Id
    @Column(name = "wallet_address", length = 42, nullable = false, updatable = false)
    private String walletAddress;

    @Column(name = "nonce", length = 32, nullable = false)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected WalletLoginNonce() {
    }

    public WalletLoginNonce(String walletAddress, String nonce, Instant expiresAt) {
        this.walletAddress = walletAddress;
        this.nonce = nonce;
        this.expiresAt = expiresAt;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getNonce() {
        return nonce;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
