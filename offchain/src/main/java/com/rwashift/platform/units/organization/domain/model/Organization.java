package com.rwashift.platform.units.organization.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The tenant root. Every issuer-side resource in every other unit (Asset, LegalStructure,
 * Offering, ...) carries this id as {@code organizationId} and must be filtered by it — see
 * {@link com.rwashift.platform.shared.domain.TenantScopedEntity} and
 * {@link com.rwashift.platform.shared.security.TenantContext}. {@code Organization} itself is
 * the tenant, so it does not carry an {@code organizationId} of its own.
 */
@Entity
@Table(name = "organization")
public class Organization extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "legal_name", nullable = false, length = 300)
    private String legalName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "country", length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    protected Organization() {
    }

    public Organization(String legalName, String displayName, String countryCode) {
        this.legalName = legalName;
        this.displayName = displayName;
        this.countryCode = countryCode;
    }

    public void rename(String legalName, String displayName) {
        this.legalName = legalName;
        this.displayName = displayName;
    }

    public void suspend() {
        this.status = OrganizationStatus.SUSPENDED;
    }

    public void reactivate() {
        this.status = OrganizationStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public OrganizationStatus getStatus() {
        return status;
    }
}
