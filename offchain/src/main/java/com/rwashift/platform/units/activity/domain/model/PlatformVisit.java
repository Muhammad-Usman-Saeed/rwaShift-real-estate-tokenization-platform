package com.rwashift.platform.units.activity.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per user per calendar day (see the {@code uq_platform_visit_user_date} unique
 * constraint) — recorded by a beacon call the frontend fires once a session is active (see
 * {@code ActivityApplicationService#recordVisit}). Deliberately not one row per request/session:
 * that would make "how many distinct people visited this week" an expensive DISTINCT scan instead
 * of a plain row count, and would grow the table far faster than the daily grain this exists to
 * report on needs.
 */
@Entity
@Table(name = "platform_visit")
public class PlatformVisit {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "user_id", length = 26, nullable = false, updatable = false)
    private String userId;

    @Column(name = "role", length = 32, nullable = false, updatable = false)
    private String role;

    @Column(name = "visit_date", nullable = false, updatable = false)
    private LocalDate visitDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PlatformVisit() {
    }

    public PlatformVisit(String userId, String role, LocalDate visitDate) {
        this.userId = userId;
        this.role = role;
        this.visitDate = visitDate;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
