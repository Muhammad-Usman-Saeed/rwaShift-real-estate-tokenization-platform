package com.rwashift.platform.units.tokenization.api.dto;

import java.math.BigDecimal;

/**
 * One organization's on-chain signing wallet plus its current native-token balance — the admin
 * "who needs funding" view. {@code balanceEth} is a live {@code eth_getBalance} read, not cached,
 * so it always reflects the current chain state.
 */
public record OrganizationWalletResponse(
        String organizationId,
        String walletAddress,
        BigDecimal balanceEth
) {
}
