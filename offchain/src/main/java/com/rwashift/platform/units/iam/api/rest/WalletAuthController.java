package com.rwashift.platform.units.iam.api.rest;

import com.rwashift.platform.units.iam.api.dto.WalletNonceResponse;
import com.rwashift.platform.units.iam.application.service.WalletAuthenticationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Under {@code /api/v1/public/**} — reachable with no session/bearer token, same as any other
 * pre-login endpoint. The actual sign-in submission is a separate, non-REST endpoint
 * ({@code POST /login/wallet}, see {@code WalletLoginFilter}) since it needs to establish a
 * Spring Security session, not just return JSON — see {@code ResourceServerConfig}.
 */
@RestController
@RequestMapping("/api/v1/public/auth/wallet")
public class WalletAuthController {

    private final WalletAuthenticationService walletAuthenticationService;

    public WalletAuthController(WalletAuthenticationService walletAuthenticationService) {
        this.walletAuthenticationService = walletAuthenticationService;
    }

    @GetMapping("/nonce")
    public WalletNonceResponse nonce(@RequestParam String address) {
        return new WalletNonceResponse(walletAuthenticationService.generateSignInMessage(address));
    }
}
