package com.rwashift.platform.units.activity.infrastructure.persistence;

import com.rwashift.platform.units.activity.domain.model.PlatformVisit;
import com.rwashift.platform.units.activity.domain.repository.PlatformVisitRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Repository;

@Repository
class PlatformVisitRepositoryAdapter implements PlatformVisitRepository {

    private final PlatformVisitJpaRepository jpaRepository;

    PlatformVisitRepositoryAdapter(PlatformVisitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByUserIdAndVisitDate(String userId, LocalDate visitDate) {
        return jpaRepository.existsByUserIdAndVisitDate(userId, visitDate);
    }

    @Override
    public PlatformVisit save(PlatformVisit visit) {
        return jpaRepository.save(visit);
    }

    @Override
    public long countDistinctUsersSince(LocalDate since) {
        return jpaRepository.countDistinctUsersSince(since);
    }

    @Override
    public long countDistinctUsersSinceForRole(LocalDate since, String role) {
        return jpaRepository.countDistinctUsersSinceForRole(since, role);
    }
}
