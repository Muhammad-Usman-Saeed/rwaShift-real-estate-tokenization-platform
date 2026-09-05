package com.rwashift.platform.units.investor.api.rest;

import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.investor.api.dto.InvestorResponse;
import com.rwashift.platform.units.investor.api.dto.LinkWalletRequest;
import com.rwashift.platform.units.investor.api.dto.OnboardInvestorRequest;
import com.rwashift.platform.units.investor.application.command.OnboardInvestorCommand;
import com.rwashift.platform.units.investor.application.service.InvestorApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investors")
public class InvestorController {

    private final InvestorApplicationService investorApplicationService;

    public InvestorController(InvestorApplicationService investorApplicationService) {
        this.investorApplicationService = investorApplicationService;
    }

    @PostMapping
    public ResponseEntity<InvestorResponse> onboard(TenantContext tenantContext,
            @Valid @RequestBody OnboardInvestorRequest request) {
        if (!tenantContext.userId().equals(request.userId()) && !tenantContext.isPlatformAdmin()) {
            throw new AccessDeniedException("May only onboard an investor profile for yourself");
        }
        var investor = investorApplicationService.onboardInvestor(new OnboardInvestorCommand(
                request.userId(), request.investorType(), request.displayName(), request.countryCode(),
                request.dateOfBirth(), request.entityRegistrationNumber(), request.primaryWalletAddress()));
        return ResponseEntity.created(URI.create("/api/v1/investors/" + investor.getId())).body(InvestorResponse.from(investor));
    }

    @GetMapping("/{investorId}")
    public InvestorResponse get(TenantContext tenantContext, @PathVariable String investorId) {
        if (!investorId.equals(tenantContext.investorId()) && !tenantContext.isPlatformAdmin()
                && !tenantContext.hasRole("COMPLIANCE_OFFICER")) {
            throw new AccessDeniedException("May only view your own investor profile");
        }
        return InvestorResponse.from(investorApplicationService.getInvestor(investorId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INVESTOR')")
    public InvestorResponse me(TenantContext tenantContext) {
        if (tenantContext.investorId() == null) {
            throw new NotFoundException("Investor", "me");
        }
        return InvestorResponse.from(investorApplicationService.getInvestor(tenantContext.investorId()));
    }

    /** Link (or re-link) the caller's own wallet — separate from the wallet address collected at onboarding. */
    @PostMapping("/me/wallet")
    @PreAuthorize("hasRole('INVESTOR')")
    public InvestorResponse linkWallet(TenantContext tenantContext, @Valid @RequestBody LinkWalletRequest request) {
        return InvestorResponse.from(investorApplicationService.linkWallet(tenantContext.investorId(), request.walletAddress()));
    }

    /** Platform-wide investor directory, for admin/compliance review screens. */
    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','COMPLIANCE_OFFICER')")
    public List<InvestorResponse> list() {
        return investorApplicationService.listInvestors().stream().map(InvestorResponse::from).toList();
    }
}
