package com.rwashift.platform.units.investment.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.investment.api.dto.CryptoPaymentInstructionsResponse;
import com.rwashift.platform.units.investment.api.dto.InitiateInvestmentRequest;
import com.rwashift.platform.units.investment.api.dto.InvestmentResponse;
import com.rwashift.platform.units.investment.application.command.InitiateInvestmentCommand;
import com.rwashift.platform.units.investment.application.service.InvestmentApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investments")
public class InvestmentController {

    private final InvestmentApplicationService investmentApplicationService;

    public InvestmentController(InvestmentApplicationService investmentApplicationService) {
        this.investmentApplicationService = investmentApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INVESTOR')")
    public ResponseEntity<InvestmentResponse> initiate(TenantContext tenantContext,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody InitiateInvestmentRequest request) {
        var investment = investmentApplicationService.initiateInvestment(new InitiateInvestmentCommand(
                tenantContext.investorId(), request.offeringId(), request.amount(), idempotencyKey, request.paymentMethod()));
        return ResponseEntity.created(URI.create("/api/v1/investments/" + investment.getId()))
                .body(InvestmentResponse.from(investment));
    }

    @PostMapping("/{investmentId}/recheck-eligibility")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public InvestmentResponse recheckEligibility(@PathVariable String investmentId) {
        return InvestmentResponse.from(investmentApplicationService.recheckEligibility(investmentId));
    }

    @PostMapping("/{investmentId}/confirm-payment")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public InvestmentResponse confirmPayment(@PathVariable String investmentId) {
        return InvestmentResponse.from(investmentApplicationService.confirmPayment(investmentId));
    }

    @GetMapping("/{investmentId}")
    public InvestmentResponse get(TenantContext tenantContext, @PathVariable String investmentId) {
        var investment = investmentApplicationService.getInvestment(investmentId);
        boolean owner = investment.getInvestorId().equals(tenantContext.investorId());
        boolean issuerSide = investment.getOrganizationId().equals(tenantContext.organizationId());
        if (!owner && !issuerSide && !tenantContext.isPlatformAdmin()) {
            throw new AccessDeniedException("May not view this investment");
        }
        return InvestmentResponse.from(investment);
    }

    /** Investor-only, and only for their own investment, and only before payment is confirmed — see {@code InvestmentStatus}'s Javadoc. */
    @PostMapping("/{investmentId}/cancel")
    @PreAuthorize("hasRole('INVESTOR')")
    public InvestmentResponse cancel(TenantContext tenantContext, @PathVariable String investmentId) {
        var existing = investmentApplicationService.getInvestment(investmentId);
        if (!existing.getInvestorId().equals(tenantContext.investorId())) {
            throw new AccessDeniedException("May not cancel this investment");
        }
        return InvestmentResponse.from(investmentApplicationService.cancelInvestment(investmentId));
    }

    /** Investor-only, and only for their own investment — deposit address + expected amount for the CRYPTO_WALLET flow. */
    @GetMapping("/{investmentId}/crypto-payment-instructions")
    @PreAuthorize("hasRole('INVESTOR')")
    public CryptoPaymentInstructionsResponse cryptoPaymentInstructions(TenantContext tenantContext, @PathVariable String investmentId) {
        var investment = investmentApplicationService.getInvestment(investmentId);
        if (!investment.getInvestorId().equals(tenantContext.investorId())) {
            throw new AccessDeniedException("May not view this investment");
        }
        return investmentApplicationService.getCryptoPaymentInstructions(investmentId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('INVESTOR')")
    public List<InvestmentResponse> mine(TenantContext tenantContext) {
        return investmentApplicationService.listForInvestor(tenantContext.investorId()).stream()
                .map(InvestmentResponse::from)
                .toList();
    }

    /** Platform admins get every investment platform-wide (needed to find investments awaiting demo payment confirmation); issuer-side roles get their own organization's. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ISSUER_OPERATOR','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public List<InvestmentResponse> forOrganization(TenantContext tenantContext) {
        return investmentApplicationService.listForOrganization(tenantContext.organizationId()).stream()
                .map(InvestmentResponse::from)
                .toList();
    }
}
