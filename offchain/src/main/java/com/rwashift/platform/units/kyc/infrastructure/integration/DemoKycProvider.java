package com.rwashift.platform.units.kyc.infrastructure.integration;

import com.rwashift.platform.units.kyc.application.port.KycProvider;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** V1 demo implementation of {@link KycProvider} — simulates a vendor correlation reference. */
@Component
class DemoKycProvider implements KycProvider {

    @Override
    public String initiateVerification(String investorId) {
        return "demo-kyc-" + UUID.randomUUID();
    }
}
