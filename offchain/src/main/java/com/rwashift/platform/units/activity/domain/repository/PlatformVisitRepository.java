package com.rwashift.platform.units.activity.domain.repository;

import com.rwashift.platform.units.activity.domain.model.PlatformVisit;
import java.time.LocalDate;

public interface PlatformVisitRepository {

    boolean existsByUserIdAndVisitDate(String userId, LocalDate visitDate);

    PlatformVisit save(PlatformVisit visit);

    long countDistinctUsersSince(LocalDate since);

    long countDistinctUsersSinceForRole(LocalDate since, String role);
}
