package com.rwashift.platform.shared.security;

/**
 * A wallet-authenticated {@code PlatformUser} still needs an email (unique, NOT NULL column) so
 * it fits the exact same shape every other login path produces — see
 * {@code WalletAuthenticationProvider}'s Javadoc for why that matters. There's no real email to
 * use, so we deterministically synthesize one from the (lowercased) wallet address; it's never
 * shown to the user and never used to send mail.
 */
public final class WalletIdentity {

    private static final String SYNTHETIC_EMAIL_DOMAIN = "@wallet.rwashift.local";

    private WalletIdentity() {
    }

    public static String normalize(String walletAddress) {
        return walletAddress.toLowerCase();
    }

    public static String syntheticEmail(String walletAddress) {
        return normalize(walletAddress) + SYNTHETIC_EMAIL_DOMAIN;
    }

    public static String shortAddress(String walletAddress) {
        String normalized = normalize(walletAddress);
        if (normalized.length() <= 10) {
            return normalized;
        }
        return normalized.substring(0, 6) + "…" + normalized.substring(normalized.length() - 4);
    }
}
