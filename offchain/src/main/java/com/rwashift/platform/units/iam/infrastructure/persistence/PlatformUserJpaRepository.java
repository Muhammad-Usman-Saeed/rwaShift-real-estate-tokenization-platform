package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlatformUserJpaRepository extends JpaRepository<PlatformUser, String> {

    Optional<PlatformUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
