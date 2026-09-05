package com.rwashift.platform.units.iam.infrastructure.security;

import com.rwashift.platform.shared.security.WalletIdentity;
import com.rwashift.platform.shared.security.WalletSignatureVerifier;
import com.rwashift.platform.units.iam.application.service.UserProvisioningService;
import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import com.rwashift.platform.units.iam.domain.repository.WalletLoginNonceRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies a wallet signature and turns it into a fully-authenticated {@link Authentication},
 * registered into the same {@code AuthenticationManager} as the existing username/password
 * provider (see {@code ResourceServerConfig#authenticationManager}). Deliberately produces a
 * principal of the exact same type ({@link User}, same as {@code PlatformUserDetailsService})
 * carrying the wallet-account's real email, wrapped in a plain
 * {@code UsernamePasswordAuthenticationToken} rather than a custom type — this is what lets every
 * downstream piece (session handling, {@code TokenClaimsCustomizer}, the OAuth2
 * authorization-code exchange and its Jackson-persisted authorization record, refresh tokens)
 * stay completely unaware that a signature was involved instead of a password. The JWT that comes
 * out the other end is issued by the same code, with the same claims, as a password login — see
 * the architecture discussion this implements.
 */
@Component
public class WalletAuthenticationProvider implements AuthenticationProvider {

    private final WalletLoginNonceRepository nonceRepository;
    private final UserProvisioningService userProvisioningService;

    public WalletAuthenticationProvider(WalletLoginNonceRepository nonceRepository, UserProvisioningService userProvisioningService) {
        this.nonceRepository = nonceRepository;
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        WalletAuthenticationToken token = (WalletAuthenticationToken) authentication;
        String claimedAddress = WalletIdentity.normalize(token.getWalletAddress());

        String recoveredAddress;
        try {
            recoveredAddress = WalletSignatureVerifier.recoverAddress(token.getMessage(), token.getSignature());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Could not verify wallet signature", e);
        }
        if (!recoveredAddress.equalsIgnoreCase(claimedAddress)) {
            throw new BadCredentialsException("Signature does not match the connected wallet");
        }

        // Consuming the nonce here (not after user provisioning) means a replayed request always
        // fails at this check, before it could ever create or look up an account a second time.
        var nonce = nonceRepository.findByWalletAddress(claimedAddress)
                .orElseThrow(() -> new BadCredentialsException("Sign-in request not found or already used — please try again"));
        boolean expired = nonce.isExpired();
        boolean nonceMatches = token.getMessage().contains(nonce.getNonce());
        nonceRepository.delete(nonce);
        if (expired || !nonceMatches) {
            throw new BadCredentialsException("Sign-in request expired — please try again");
        }

        PlatformUser user = userProvisioningService.findOrCreateWalletUser(claimedAddress);
        Collection<? extends GrantedAuthority> authorities = authorities(user);
        User principal = new User(user.getEmail(), user.getPasswordHash(), user.isEnabled(), true, true, true, authorities);
        // Deliberately NOT WalletAuthenticationToken here: JdbcOAuth2AuthorizationService
        // Jackson-serializes the Authentication into the persisted authorization row, and a
        // custom Authentication subtype isn't on Spring Security's deserialization allowlist —
        // /oauth2/token would 500 trying to read it back. UsernamePasswordAuthenticationToken is
        // a core, pre-allowlisted type, and by this point "how the user proved who they are" is
        // no longer relevant information — only the resulting principal/authorities are.
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WalletAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static Collection<? extends GrantedAuthority> authorities(PlatformUser user) {
        if (user.getGlobalRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getGlobalRole().name()));
    }
}
