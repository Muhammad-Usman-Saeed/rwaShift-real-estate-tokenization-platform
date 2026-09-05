package com.rwashift.platform.units.iam.infrastructure.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * The pre-auth {@code Authentication} carrier for wallet sign-in: carries the raw claim
 * (address/signature/message) into {@link WalletAuthenticationProvider#authenticate}. There's no
 * "authenticated" variant of this type — the provider returns a plain
 * {@code UsernamePasswordAuthenticationToken} instead once verification succeeds, since that's a
 * core Spring Security type already on the Jackson deserialization allowlist
 * {@code JdbcOAuth2AuthorizationService} needs (see the provider's Javadoc for why that matters).
 */
public class WalletAuthenticationToken extends AbstractAuthenticationToken {

    private final String walletAddress;
    private final String signature;
    private final String message;

    private WalletAuthenticationToken(String walletAddress, String signature, String message) {
        super(List.of());
        this.walletAddress = walletAddress;
        this.signature = signature;
        this.message = message;
        setAuthenticated(false);
    }

    public static WalletAuthenticationToken unauthenticated(String walletAddress, String signature, String message) {
        return new WalletAuthenticationToken(walletAddress, signature, message);
    }

    @Override
    public Object getCredentials() {
        return signature;
    }

    @Override
    public Object getPrincipal() {
        return walletAddress;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getSignature() {
        return signature;
    }

    public String getMessage() {
        return message;
    }
}
