package com.rwashift.platform.units.investment.api.dto;

import com.rwashift.platform.units.investment.domain.model.Investment;
import java.math.BigDecimal;
import java.time.Instant;

public record InvestmentResponse(
        String id,
        String organizationId,
        String investorId,
        String offeringId,
        BigDecimal amount,
        String currency,
        BigDecimal unitPrice,
        long units,
        String status,
        String paymentStatus,
        String tokenIssuanceStatus,
        String failureReason,
        String paymentMethod,
        String paymentTxHash,
        String payerWalletAddress,
        Instant createdAt,
        Instant updatedAt
) {
    public static InvestmentResponse from(Investment investment) {
        return new InvestmentResponse(
                investment.getId(), investment.getOrganizationId(), investment.getInvestorId(), investment.getOfferingId(),
                investment.getAmount(), investment.getCurrency(), investment.getUnitPrice(), investment.getUnits(),
                investment.getStatus().name(), investment.getPaymentStatus().name(), investment.getTokenIssuanceStatus().name(),
                investment.getFailureReason(), investment.getPaymentMethod().name(), investment.getPaymentTxHash(),
                investment.getPayerWalletAddress(), investment.getCreatedAt(), investment.getUpdatedAt());
    }
}
