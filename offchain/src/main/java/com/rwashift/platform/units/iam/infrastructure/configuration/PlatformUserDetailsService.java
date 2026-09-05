package com.rwashift.platform.units.iam.infrastructure.configuration;

import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adapts {@link PlatformUser} to Spring Security's own {@link User} (never a custom
 * {@code UserDetails} implementation — see {@code TokenClaimsCustomizer}'s Javadoc for why:
 * {@code JdbcOAuth2AuthorizationService} needs to Jackson-serialize the principal, and a custom
 * class isn't on Spring Security's deserialization allowlist without extra mixin wiring this
 * avoids entirely). Authorities here only cover the user's global role (if any); org-scoped roles
 * are resolved and placed on the access token by {@code TokenClaimsCustomizer} at token-issuance
 * time, not at login time.
 */
@Service
class PlatformUserDetailsService implements UserDetailsService {

    private final PlatformUserRepository userRepository;

    PlatformUserDetailsService(PlatformUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        PlatformUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
        return new User(user.getEmail(), user.getPasswordHash(), user.isEnabled(), true, true, true, authorities(user));
    }

    private static Collection<? extends GrantedAuthority> authorities(PlatformUser user) {
        if (user.getGlobalRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getGlobalRole().name()));
    }
}
