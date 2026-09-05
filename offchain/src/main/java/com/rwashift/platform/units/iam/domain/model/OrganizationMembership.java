package com.rwashift.platform.units.iam.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Grants a {@link PlatformUser} a {@link PlatformRole} within one organization. This is the
 * join between "who can log in" and "what tenant/role they act as" — the access token's
 * {@code org_id}/{@code roles} claims are minted from this at token-issuance time (see IAM's
 * JWT customizer), and every other unit trusts those claims rather than re-querying IAM.
 */
@Entity
@Table(name = "organization_membership",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
public class OrganizationMembership extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "user_id", length = 26, nullable = false, updatable = false)
    private String userId;

    @Column(name = "organization_id", length = 26, nullable = false, updatable = false)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private PlatformRole role;

    protected OrganizationMembership() {
    }

    public OrganizationMembership(String userId, String organizationId, PlatformRole role) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public PlatformRole getRole() {
        return role;
    }
}
