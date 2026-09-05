package com.rwashift.platform.units.iam.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Maps one external login credential (a wallet address, an email used for a password login,
 * eventually a Google subject) to exactly one {@link PlatformUser} — the "one account, several
 * ways in" model. {@code (provider, identifier)} is unique platform-wide: the same wallet address
 * or email can never resolve to two different accounts. A single user can have several rows here,
 * one per provider they've used.
 *
 * <p>Not named {@code InvestorIdentity} even though investors are the primary audience for
 * multi-method login: identity resolution happens at the {@link PlatformUser} (account/login)
 * layer, which exists independently of — and before — any {@code Investor} business profile. An
 * org-provisioned issuer user could just as validly accumulate identities here later.
 */
@Entity
@Table(name = "platform_user_identity")
public class PlatformUserIdentity {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "user_id", length = 26, nullable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32, updatable = false)
    private IdentityProvider provider;

    @Column(name = "identifier", nullable = false, length = 320, updatable = false)
    private String identifier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PlatformUserIdentity() {
    }

    public PlatformUserIdentity(String userId, IdentityProvider provider, String identifier) {
        this.userId = userId;
        this.provider = provider;
        this.identifier = identifier;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public IdentityProvider getProvider() {
        return provider;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
