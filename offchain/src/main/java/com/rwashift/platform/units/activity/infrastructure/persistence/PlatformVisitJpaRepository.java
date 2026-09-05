package com.rwashift.platform.units.activity.infrastructure.persistence;

import com.rwashift.platform.units.activity.domain.model.PlatformVisit;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PlatformVisitJpaRepository extends JpaRepository<PlatformVisit, String> {

    boolean existsByUserIdAndVisitDate(String userId, LocalDate visitDate);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM PlatformVisit v WHERE v.visitDate >= :since")
    long countDistinctUsersSince(@Param("since") LocalDate since);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM PlatformVisit v WHERE v.visitDate >= :since AND v.role = :role")
    long countDistinctUsersSinceForRole(@Param("since") LocalDate since, @Param("role") String role);
}
