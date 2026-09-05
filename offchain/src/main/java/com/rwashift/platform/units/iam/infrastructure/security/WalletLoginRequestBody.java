package com.rwashift.platform.units.iam.infrastructure.security;

/** Body of {@code POST /login/wallet} — see {@link WalletLoginFilter}. */
record WalletLoginRequestBody(String walletAddress, String signature, String message) {
}
