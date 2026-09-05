package com.rwashift.platform.units.reporting.application.service;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.service.AssetApplicationService;
import com.rwashift.platform.units.distribution.application.service.DistributionApplicationService;
import com.rwashift.platform.units.investment.application.service.InvestmentApplicationService;
import com.rwashift.platform.units.investment.domain.model.Investment;
import com.rwashift.platform.units.offering.application.service.OfferingApplicationService;
import com.rwashift.platform.units.offering.domain.model.Offering;
import com.rwashift.platform.units.offering.domain.model.OfferingStatus;
import com.rwashift.platform.units.ownership.application.service.OwnershipApplicationService;
import com.rwashift.platform.units.reporting.api.dto.InvestorSummaryResponse;
import com.rwashift.platform.units.reporting.api.dto.IssuerSummaryResponse;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregation over other units' existing application-service query methods (never
 * their JPA repositories/entities directly) — see README for the "reporting respects capability
 * ownership" rationale. Optimized/denormalized read models are a natural next step if these
 * aggregations become a performance bottleneck; V1 keeps them simple and obviously correct.
 */
@Service
public class ReportingApplicationService {

    private final AssetApplicationService assetApplicationService;
    private final OfferingApplicationService offeringApplicationService;
    private final InvestmentApplicationService investmentApplicationService;
    private final OwnershipApplicationService ownershipApplicationService;
    private final DistributionApplicationService distributionApplicationService;

    public ReportingApplicationService(AssetApplicationService assetApplicationService,
            OfferingApplicationService offeringApplicationService, InvestmentApplicationService investmentApplicationService,
            OwnershipApplicationService ownershipApplicationService, DistributionApplicationService distributionApplicationService) {
        this.assetApplicationService = assetApplicationService;
        this.offeringApplicationService = offeringApplicationService;
        this.investmentApplicationService = investmentApplicationService;
        this.ownershipApplicationService = ownershipApplicationService;
        this.distributionApplicationService = distributionApplicationService;
    }

    @Transactional(readOnly = true)
    public IssuerSummaryResponse issuerSummary(TenantContext caller) {
        var assets = assetApplicationService.listAssets(caller);
        var offerings = offeringApplicationService.listOfferings(caller);
        var investments = investmentApplicationService.listForOrganization(caller.organizationId());
        var distributions = distributionApplicationService.listForOrganization(caller);

        BigDecimal totalValuation = assets.stream().map(a -> a.getValuation()).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeOfferings = offerings.stream().filter(o -> o.getStatus() == OfferingStatus.OPEN).count();
        BigDecimal totalRaised = investments.stream()
                .filter(i -> i.getStatus() == com.rwashift.platform.units.investment.domain.model.InvestmentStatus.SETTLED)
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long distinctInvestors = investments.stream().map(Investment::getInvestorId).distinct().count();
        long unitsIssued = offerings.stream().mapToLong(Offering::getUnitsIssued).sum();
        long unitsAvailable = offerings.stream().mapToLong(Offering::getUnitsAvailable).sum();

        return new IssuerSummaryResponse(caller.organizationId(), assets.size(), totalValuation, activeOfferings,
                totalRaised, distinctInvestors, unitsIssued, unitsAvailable, distributions.size());
    }

    @Transactional(readOnly = true)
    public InvestorSummaryResponse investorSummary(String investorId) {
        var investments = investmentApplicationService.listForInvestor(investorId);
        var holdings = ownershipApplicationService.getOwnershipForInvestor(investorId);

        BigDecimal totalInvested = investments.stream().map(Investment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigInteger totalUnits = holdings.stream().map(h -> h.getUnits()).reduce(BigInteger.ZERO, BigInteger::add);
        long distinctOfferings = holdings.stream().map(h -> h.getOfferingId()).distinct().count();

        return new InvestorSummaryResponse(investorId, totalInvested, investments.size(), totalUnits, distinctOfferings);
    }
}
