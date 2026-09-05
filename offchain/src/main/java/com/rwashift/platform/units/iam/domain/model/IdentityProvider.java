package com.rwashift.platform.units.iam.domain.model;

/**
 * How a {@link PlatformUser} proved who they are for a given login method — see
 * {@link PlatformUserIdentity}. One account can accumulate several of these (password today,
 * wallet today, Google later) without ever becoming multiple accounts.
 */
public enum IdentityProvider {
    LOCAL,
    WALLET
}
