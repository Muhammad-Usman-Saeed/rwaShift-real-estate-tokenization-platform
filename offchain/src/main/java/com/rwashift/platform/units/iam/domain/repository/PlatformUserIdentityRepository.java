package com.rwashift.platform.units.iam.domain.repository;

import com.rwashift.platform.units.iam.domain.model.IdentityProvider;
import com.rwashift.platform.units.iam.domain.model.PlatformUserIdentity;
import java.util.Optional;

public interface PlatformUserIdentityRepository {

    PlatformUserIdentity save(PlatformUserIdentity identity);

    Optional<PlatformUserIdentity> findByProviderAndIdentifier(IdentityProvider provider, String identifier);
}
