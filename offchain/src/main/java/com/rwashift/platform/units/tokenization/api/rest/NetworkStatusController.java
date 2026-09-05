package com.rwashift.platform.units.tokenization.api.rest;

import com.rwashift.platform.units.tokenization.api.dto.NetworkStatusResponse;
import com.rwashift.platform.units.tokenization.application.service.NetworkStatusApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated by design (see {@code ResourceServerConfig}'s {@code /api/v1/public/**}
 * matcher) — backs the network-insight panel shown on the login screen, before a session exists.
 * Every field it returns is public on-chain information; see {@code
 * NetworkStatusApplicationService}'s Javadoc for what that boundary excludes.
 */
@RestController
@RequestMapping("/api/v1/public")
public class NetworkStatusController {

    private final NetworkStatusApplicationService networkStatusApplicationService;

    public NetworkStatusController(NetworkStatusApplicationService networkStatusApplicationService) {
        this.networkStatusApplicationService = networkStatusApplicationService;
    }

    @GetMapping("/network-status")
    public NetworkStatusResponse networkStatus() {
        return NetworkStatusResponse.from(networkStatusApplicationService.getStatus());
    }
}
