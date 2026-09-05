package com.rwashift.platform.units.iam.domain.repository;

import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import java.util.Optional;

/**
 * Port owned by the IAM domain. The Spring Data JPA implementation lives in
 * {@code infrastructure.persistence} — no other unit is allowed to depend on that
 * implementation or on the {@code PlatformUser} JPA entity directly.
 */
public interface PlatformUserRepository {

    PlatformUser save(PlatformUser user);

    Optional<PlatformUser> findById(String id);

    Optional<PlatformUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
