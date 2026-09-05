package com.rwashift.platform.units.iam.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A platform login identity. Deliberately NOT tenant-scoped by itself — a user's organization
 * access is expressed through {@link OrganizationMembership} rows, since a user could in theory
 * belong to more than one organization (e.g. a compliance consultant across issuers). Wallet
 * identity is separate: see {@code units.investor} — a {@code PlatformUser} never has a wallet
 * address of its own.
 */
@Entity
@Table(name = "platform_user")
public class PlatformUser extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PlatformUserStatus status = PlatformUserStatus.ACTIVE;

    /**
     * Set only for roles that are NOT organization-scoped: {@code PLATFORM_ADMIN} (acts
     * platform-wide) and {@code INVESTOR} (acts as themselves, scoped by {@link #investorId}
     * rather than an organization). Issuer-side roles come exclusively from
     * {@link OrganizationMembership} rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", length = 32)
    private PlatformRole globalRole;

    /**
     * Set once an investor user has completed investor onboarding (see
     * {@code units.investor}). IAM stores only this opaque id — never investor profile data —
     * so the access token can carry an {@code investor_id} claim without IAM depending on the
     * Investor unit's domain model.
     */
    @Column(name = "investor_id", length = 26)
    private String investorId;

    protected PlatformUser() {
    }

    public PlatformUser(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PlatformUserStatus getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return status == PlatformUserStatus.ACTIVE;
    }

    public PlatformRole getGlobalRole() {
        return globalRole;
    }

    public void assignGlobalRole(PlatformRole role) {
        this.globalRole = role;
    }

    public String getInvestorId() {
        return investorId;
    }

    public void linkInvestor(String investorId) {
        this.investorId = investorId;
    }
}
