package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PlatformUserRepositoryAdapter implements PlatformUserRepository {

    private final PlatformUserJpaRepository jpaRepository;

    PlatformUserRepositoryAdapter(PlatformUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformUser save(PlatformUser user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<PlatformUser> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<PlatformUser> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
