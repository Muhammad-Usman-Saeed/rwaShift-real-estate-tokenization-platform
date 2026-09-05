package com.rwashift.platform.units.tokenization.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One organization's own on-chain signing key — the address that becomes {@code issuerAdmin} and
 * {@code TOKEN_AGENT} for every offering that organization tokenizes, in place of the platform's
 * single shared agent wallet (see docs/critical-analysis.md — "single shared on-chain agent
 * wallet" risk). {@code encryptedPrivateKey} is AES-GCM ciphertext (base64, IV prepended) produced
 * by {@code OrganizationWalletEncryptionService}; the plaintext key never touches this entity or
 * the database. One row per organization, created lazily on that organization's first tokenization
 * action ({@code OrganizationWalletProvisioningService#getOrCreateWallet}).
 */
@Entity
@Table(name = "organization_wallet")
public class OrganizationWallet extends Auditable {

    @Column(name = "id", length = 26, nullable = false, updatable = false)
    @jakarta.persistence.Id
    private String id = IdGenerator.newId();

    @Column(name = "organization_id", nullable = false, length = 26, unique = true)
    private String organizationId;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "encrypted_private_key", nullable = false, length = 512)
    private String encryptedPrivateKey;

    protected OrganizationWallet() {
    }

    public OrganizationWallet(String organizationId, String walletAddress, String encryptedPrivateKey) {
        this.organizationId = organizationId;
        this.walletAddress = walletAddress;
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public String getId() {
        return id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }
}
