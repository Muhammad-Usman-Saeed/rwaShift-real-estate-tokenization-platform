package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.IdentityProvider;
import com.rwashift.platform.units.iam.domain.model.PlatformUserIdentity;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserIdentityRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PlatformUserIdentityRepositoryAdapter implements PlatformUserIdentityRepository {

    private final PlatformUserIdentityJpaRepository jpaRepository;

    PlatformUserIdentityRepositoryAdapter(PlatformUserIdentityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformUserIdentity save(PlatformUserIdentity identity) {
        return jpaRepository.save(identity);
    }

    @Override
    public Optional<PlatformUserIdentity> findByProviderAndIdentifier(IdentityProvider provider, String identifier) {
        return jpaRepository.findByProviderAndIdentifier(provider, identifier);
    }
}
