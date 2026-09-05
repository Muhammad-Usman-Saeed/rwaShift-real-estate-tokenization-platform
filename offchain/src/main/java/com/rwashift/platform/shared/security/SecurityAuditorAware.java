package com.rwashift.platform.shared.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Resolves the actor Spring Data JPA auditing stamps into {@code created_by}/{@code updated_by}
 * (see {@link com.rwashift.platform.shared.domain.Auditable}).
 *
 * <p>Reads {@link SecurityContextHolder} rather than {@link TenantContext}: JPA auditing fires
 * from the persistence layer at flush time, which also has to work from contexts with no
 * {@code TenantContext} in scope at all — {@code @Scheduled} jobs
 * ({@code BlockchainTransactionManager}, {@code BlockchainEventIndexer}) and boot-time seeding
 * ({@code DemoDataSeeder}). Those fall back to {@code "system"}.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_ACTOR = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return Optional.of(jwt.getToken().getSubject());
        }
        return Optional.of(SYSTEM_ACTOR);
    }
}
