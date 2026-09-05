package com.rwashift.platform.units.iam.infrastructure.configuration;

import com.rwashift.platform.units.iam.domain.model.OrganizationMembership;
import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import com.rwashift.platform.units.iam.domain.repository.OrganizationMembershipRepository;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Bakes the tenant/authorization claims every downstream unit relies on
 * ({@code org_id}, {@code roles}, {@code investor_id}) into the access token at issuance time,
 * so resource servers never need to call back into IAM per-request. V1 assumes a single
 * organization per issuer-side user (the common case); a user with more than one membership
 * would need a {@code /oauth2/authorize?org=...} org-selection step, which is out of scope here.
 *
 * <p>The principal here is Spring Security's own {@link User} (set by
 * {@code PlatformUserDetailsService}), not a custom {@code UserDetails} — {@code
 * JdbcOAuth2AuthorizationService} round-trips the principal through Jackson to store it in
 * {@code oauth2_authorization}, and Jackson's security-hardened deserializer only allows classes
 * on an explicit allowlist. {@code User} is already on it; a custom principal class wouldn't be
 * without extra Jackson mixin configuration, so this looks the {@link PlatformUser} back up by
 * email (the {@code User}'s username) instead of carrying it on the principal directly.
 */
@Component
class TokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final OrganizationMembershipRepository membershipRepository;
    private final PlatformUserRepository platformUserRepository;

    TokenClaimsCustomizer(OrganizationMembershipRepository membershipRepository, PlatformUserRepository platformUserRepository) {
        this.membershipRepository = membershipRepository;
        this.platformUserRepository = platformUserRepository;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!"access_token".equals(context.getTokenType().getValue())) {
            return;
        }
        if (!(context.getPrincipal().getPrincipal() instanceof User principal)) {
            return;
        }
        PlatformUser user = platformUserRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) {
            return;
        }
        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());

        Set<String> roles = new LinkedHashSet<>();
        if (user.getGlobalRole() != null) {
            roles.add(user.getGlobalRole().name());
        }

        String organizationId = null;
        if (!memberships.isEmpty()) {
            OrganizationMembership primary = memberships.get(0);
            organizationId = primary.getOrganizationId();
            roles.add(primary.getRole().name());
        }

        // org_id/investor_id are legitimately absent for a platform admin (no organization) or an
        // issuer-side user who hasn't onboarded as an investor — JwtClaimsSet.Builder.claim(k, v)
        // asserts v non-null, so those two are only added when actually present; every consumer
        // already treats a missing claim the same as a null one (see TenantContext, callers of
        // jwt.getClaimAsString(...)).
        String finalOrganizationId = organizationId;
        String investorId = user.getInvestorId();
        context.getClaims()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claims(claims -> {
                    if (finalOrganizationId != null) {
                        claims.put("org_id", finalOrganizationId);
                    }
                    if (investorId != null) {
                        claims.put("investor_id", investorId);
                    }
                });
    }
}
