package com.rwashift.platform.units.tokenization.application.service;

import java.math.BigInteger;

/**
 * One organization's wallet address plus a live {@code eth_getBalance} read — not a persisted
 * projection, just the pairing {@link TokenizationApplicationService#listOrganizationWallets()}
 * hands to the controller for the admin "who needs funding" view.
 */
public record OrganizationWalletBalance(String organizationId, String walletAddress, BigInteger balanceWei) {
}
