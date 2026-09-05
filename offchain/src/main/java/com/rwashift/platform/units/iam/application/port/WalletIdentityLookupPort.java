package com.rwashift.platform.units.iam.application.port;

import java.util.Optional;

/**
 * Explicit cross-unit contract for "does this wallet address already have a login identity, and
 * if so whose" — consumed by Investor so it can refuse to link a wallet to one account's investor
 * profile when that same wallet already signs in to a different account (the mirror image of
 * {@code InvestorLookupPort#findByWalletAddress}, which lets Iam resolve wallet sign-in back to an
 * existing investor-linked account). See {@code UserProvisioningService#findOrCreateWalletUser}
 * and {@code InvestorApplicationService#linkWallet}/{@code #onboardInvestor}.
 */
public interface WalletIdentityLookupPort {

    Optional<String> findUserIdByWallet(String walletAddress);
}
