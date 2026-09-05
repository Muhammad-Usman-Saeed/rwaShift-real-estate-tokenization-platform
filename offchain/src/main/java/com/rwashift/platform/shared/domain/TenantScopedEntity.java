package com.rwashift.platform.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for every JPA aggregate root that belongs to an issuer-side organization
 * (tenant). Every subclass carries {@code organizationId} and it is the subclass's
 * repository/query responsibility to always filter by it (see {@code TenantContext} and
 * per-unit repositories) — there is no cross-cutting Hibernate filter, by design, so that
 * tenant scoping is visible and testable at the query layer rather than implicit magic.
 *
 * <p>Auditing fields ({@code version}, {@code created_at}/{@code updated_at},
 * {@code created_by}/{@code updated_by}) come from {@link Auditable} and apply identically to
 * every tenant-scoped entity.
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "organization_id", length = 26, nullable = false, updatable = false)
    private String organizationId;

    protected TenantScopedEntity() {
    }

    protected TenantScopedEntity(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getId() {
        return id;
    }

    public String getOrganizationId() {
        return organizationId;
    }
}
