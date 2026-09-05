package com.rwashift.platform.units.notification.application.service;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.service.AssetApplicationService;
import com.rwashift.platform.units.asset.domain.model.Asset;
import com.rwashift.platform.units.asset.domain.model.AssetStatus;
import com.rwashift.platform.units.investment.application.service.InvestmentApplicationService;
import com.rwashift.platform.units.investor.application.service.InvestorApplicationService;
import com.rwashift.platform.units.kyc.application.service.KycApplicationService;
import com.rwashift.platform.units.kyc.domain.model.KycCase;
import com.rwashift.platform.units.kyc.domain.model.KycStatus;
import com.rwashift.platform.units.notification.api.dto.NotificationItem;
import com.rwashift.platform.units.offering.application.service.OfferingApplicationService;
import com.rwashift.platform.units.offering.domain.model.Offering;
import com.rwashift.platform.units.offering.domain.model.OfferingStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregation over other units' existing application-service query methods — same
 * "reporting respects capability ownership" rule as {@code ReportingApplicationService} (never
 * reach into another unit's repository/entities directly). There is deliberately no persisted
 * {@code notification} table: every item here is derived on read from each unit's current state
 * (an offering that's {@code UNDER_REVIEW} right now IS the notification "an offering needs your
 * approval" — no separate event log to keep in sync, no risk of a stale/duplicate row). The
 * tradeoff is that "read/dismissed" state isn't tracked server-side; the frontend keeps that in
 * localStorage against each item's stable {@code id}. If this ever needs cross-device read state
 * or delivery (email/push, via {@link com.rwashift.platform.units.notification.application.port.NotificationSender}),
 * that's the natural point to introduce real persistence.
 */
@Service
public class NotificationApplicationService {

    private final OfferingApplicationService offeringApplicationService;
    private final KycApplicationService kycApplicationService;
    private final AssetApplicationService assetApplicationService;
    private final InvestmentApplicationService investmentApplicationService;
    private final InvestorApplicationService investorApplicationService;

    public NotificationApplicationService(OfferingApplicationService offeringApplicationService,
            KycApplicationService kycApplicationService, AssetApplicationService assetApplicationService,
            InvestmentApplicationService investmentApplicationService, InvestorApplicationService investorApplicationService) {
        this.offeringApplicationService = offeringApplicationService;
        this.kycApplicationService = kycApplicationService;
        this.assetApplicationService = assetApplicationService;
        this.investmentApplicationService = investmentApplicationService;
        this.investorApplicationService = investorApplicationService;
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> listForCaller(TenantContext caller) {
        List<NotificationItem> items = new ArrayList<>();

        if (caller.isPlatformAdmin() || caller.hasRole("COMPLIANCE_OFFICER")) {
            addComplianceQueueItems(caller, items);
        }
        if (caller.hasRole("ISSUER_ADMIN") || caller.hasRole("ISSUER_OPERATOR") || caller.hasRole("ORGANIZATION_ADMIN")) {
            addIssuerActionItems(caller, items);
        }
        if (caller.hasRole("INVESTOR") && caller.investorId() != null) {
            addInvestorOpportunityItems(caller, items);
        }

        items.sort(Comparator.comparing(NotificationItem::occurredAt).reversed());
        return items;
    }

    /** Offerings awaiting approval, assets awaiting verification, KYC cases awaiting review. */
    private void addComplianceQueueItems(TenantContext caller, List<NotificationItem> items) {
        for (Offering offering : offeringApplicationService.listOfferings(caller)) {
            if (offering.getStatus() == OfferingStatus.UNDER_REVIEW) {
                items.add(new NotificationItem(
                        "offering-approval:" + offering.getId(),
                        "OFFERING_PENDING_APPROVAL",
                        "Offering awaiting approval",
                        "\"" + offering.getName() + "\" was submitted for review and needs a decision.",
                        "/admin/offering-approvals/" + offering.getId(),
                        offering.getUpdatedAt()));
            }
        }
        for (Asset asset : assetApplicationService.listAssets(caller)) {
            if (asset.getStatus() == AssetStatus.UNDER_VERIFICATION) {
                items.add(new NotificationItem(
                        "asset-verification:" + asset.getId(),
                        "ASSET_PENDING_VERIFICATION",
                        "Asset awaiting verification",
                        "\"" + asset.getName() + "\" was submitted for verification.",
                        "/admin/assets",
                        asset.getUpdatedAt()));
            }
        }
        for (KycCase kycCase : kycApplicationService.listCases()) {
            if (kycCase.getStatus() == KycStatus.SUBMITTED || kycCase.getStatus() == KycStatus.UNDER_REVIEW) {
                String investorName = investorNameOrId(kycCase.getInvestorId());
                items.add(new NotificationItem(
                        "kyc-review:" + kycCase.getId(),
                        "KYC_PENDING_REVIEW",
                        "KYC review needed",
                        investorName + "'s KYC submission is awaiting review.",
                        "/admin/kyc-reviews",
                        kycCase.getSubmittedAt()));
            }
        }
    }

    /** Offerings this issuer submitted that compliance rejected — needs revision + resubmission. */
    private void addIssuerActionItems(TenantContext caller, List<NotificationItem> items) {
        for (Offering offering : offeringApplicationService.listOfferings(caller)) {
            if (offering.getStatus() == OfferingStatus.REJECTED) {
                items.add(new NotificationItem(
                        "offering-rejected:" + offering.getId(),
                        "OFFERING_REJECTED",
                        "Offering rejected",
                        "\"" + offering.getName() + "\" was rejected and needs revision before resubmitting.",
                        "/issuer/offerings/" + offering.getId(),
                        offering.getUpdatedAt()));
            }
        }
    }

    /** Offerings now OPEN for investment that this investor hasn't put money into yet. */
    private void addInvestorOpportunityItems(TenantContext caller, List<NotificationItem> items) {
        Set<String> investedOfferingIds = investmentApplicationService.listForInvestor(caller.investorId()).stream()
                .map(investment -> investment.getOfferingId())
                .collect(Collectors.toSet());
        for (Offering offering : offeringApplicationService.listOfferings(caller)) {
            if (offering.getStatus() == OfferingStatus.OPEN && !investedOfferingIds.contains(offering.getId())) {
                items.add(new NotificationItem(
                        "offering-open:" + offering.getId(),
                        "OFFERING_OPEN_FOR_INVESTMENT",
                        "New investment opportunity",
                        "\"" + offering.getName() + "\" is now open for investment.",
                        "/investor/opportunities/" + offering.getId(),
                        offering.getUpdatedAt()));
            }
        }
    }

    private String investorNameOrId(String investorId) {
        try {
            return investorApplicationService.getInvestor(investorId).getDisplayName();
        } catch (RuntimeException ex) {
            return investorId;
        }
    }
}
