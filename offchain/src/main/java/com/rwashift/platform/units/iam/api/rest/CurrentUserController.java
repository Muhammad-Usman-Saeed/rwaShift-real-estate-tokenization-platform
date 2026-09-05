package com.rwashift.platform.units.iam.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.iam.api.dto.CurrentUserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam")
public class CurrentUserController {

    @GetMapping("/me")
    public CurrentUserResponse me(TenantContext tenantContext) {
        return new CurrentUserResponse(
                tenantContext.userId(),
                tenantContext.organizationId(),
                tenantContext.roles(),
                tenantContext.investorId());
    }
}
