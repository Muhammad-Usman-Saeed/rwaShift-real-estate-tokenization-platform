package com.rwashift.platform.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for every JPA entity in the platform, tenant-scoped or not. Stamps
 * {@code created_at}/{@code updated_at} and {@code created_by}/{@code updated_by} automatically
 * via Spring Data's {@link AuditingEntityListener} — never set by hand in a constructor or
 * mutator, so a forgotten {@code this.updatedAt = Instant.now()} in some future setter can no
 * longer leave the timestamp stale.
 *
 * <p>{@code created_by}/{@code updated_by} are a denormalized last-actor snapshot for cheap,
 * join-free reads (e.g. "who last touched this record"). They are not a substitute for the
 * {@code audit} unit's append-only log, which remains the authoritative history of every change
 * by every actor — see ADR-007 for the analogous reasoning applied to on-chain ownership. The
 * actor id comes from {@code shared.security.SecurityAuditorAware}, which resolves the current
 * JWT subject, falling back to {@code "system"} for scheduled jobs and boot-time seeding where
 * there is no authenticated request.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 26, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 26)
    private String updatedBy;

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
