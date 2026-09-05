package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.IdentityProvider;
import com.rwashift.platform.units.iam.domain.model.PlatformUserIdentity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlatformUserIdentityJpaRepository extends JpaRepository<PlatformUserIdentity, String> {

    Optional<PlatformUserIdentity> findByProviderAndIdentifier(IdentityProvider provider, String identifier);
}
