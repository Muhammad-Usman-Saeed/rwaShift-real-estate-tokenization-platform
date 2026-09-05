package com.rwashift.platform.units.activity.application.service;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.activity.api.dto.ActivityFeedItem;
import com.rwashift.platform.units.activity.api.dto.ActivityStatsResponse;
import com.rwashift.platform.units.activity.api.dto.VisitStatsResponse;
import com.rwashift.platform.units.activity.domain.model.PlatformVisit;
import com.rwashift.platform.units.activity.domain.repository.PlatformVisitRepository;
import com.rwashift.platform.units.asset.application.port.AssetLookupPort;
import com.rwashift.platform.units.audit.application.service.AuditApplicationService;
import com.rwashift.platform.units.audit.domain.model.AuditLogEntry;
import com.rwashift.platform.units.distribution.application.port.DistributionLookupPort;
import com.rwashift.platform.units.investment.application.port.InvestmentLookupPort;
import com.rwashift.platform.units.investment.application.port.InvestmentSnapshot;
import com.rwashift.platform.units.legalstructure.application.port.LegalStructureLookupPort;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import com.rwashift.platform.units.offering.application.port.OfferingSnapshot;
import com.rwashift.platform.units.organization.application.port.OrganizationLookupPort;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Powers the top-of-screen news ticker and the full "Activity" page: a platform-wide feed of
 * newsworthy events (derived from the existing append-only audit log — see
 * {@link #NEWSWORTHY_ACTIONS} for the curated subset; the raw audit log has many internal/technical
 * entries not worth surfacing here) plus visitor counts. There is deliberately no persisted
 * activity-feed table of its own — the audit log is already the append-only source of truth for
 * "what happened," so this reads it rather than duplicating it.
 *
 * <p>Visitor counts DO need their own tiny table ({@link PlatformVisit}) since nothing else in the
 * platform records logins/sessions — see {@link #recordVisit}.
 */
@Service
public class ActivityApplicationService {

    /**
     * Curated subset of audit actions worth putting in front of every user platform-wide, as
     * opposed to internal/technical steps (blockchain tx submission, eligibility re-evaluation,
     * ownership indexing) that are only meaningful in the detailed per-resource audit trail.
     */
    private static final Set<String> NEWSWORTHY_ACTIONS = Set.of(
            "ORGANIZATION_CREATED", "ASSET_CREATED", "ASSET_MARKED_UNDER_VERIFICATION", "ASSET_VERIFIED",
            "INVESTOR_ONBOARDED", "OFFERING_CREATED", "OFFERING_SUBMITTED_FOR_REVIEW", "OFFERING_APPROVED",
            "OFFERING_REJECTED", "OFFERING_TOKENIZATION_STARTED", "OFFERING_OPENED_FOR_INVESTMENT",
            "OFFERING_CLOSED", "KYC_SUBMITTED", "KYC_VERIFIED", "KYC_REJECTED", "INVESTMENT_INITIATED",
            "PAYMENT_CONFIRMED", "TOKEN_ISSUANCE_CONFIRMED", "DISTRIBUTION_CREATED", "DISTRIBUTION_COMPLETED",
            "LEGAL_STRUCTURE_ACTIVATED", "DOCUMENT_UPLOADED", "INVESTOR_SELF_SIGNUP", "WALLET_USER_PROVISIONED");

    /**
     * Templates with a {@code %s} placeholder are filled in with the real entity's display name
     * (see {@link #resolveSubject}), unquoted — e.g. {@code "Dubai Business Tower SPV Units opened
     * for investment."} rather than the generic {@code "An offering opened for investment."} Scoped to
     * Organization/Asset/Offering names deliberately: those are non-confidential, issuer-chosen
     * names an issuer *wants* publicized, unlike an investment's amount or investor identity, which
     * this platform-wide (not tenant-scoped) feed must never leak across organizations.
     */
    private static final Map<String, String> ACTION_MESSAGES = Map.ofEntries(
            Map.entry("ORGANIZATION_CREATED", "%s joined the platform."),
            Map.entry("ASSET_CREATED", "%s was added to the platform."),
            Map.entry("ASSET_MARKED_UNDER_VERIFICATION", "%s was submitted for legal verification."),
            Map.entry("ASSET_VERIFIED", "%s was legally verified."),
            Map.entry("INVESTOR_ONBOARDED", "A new investor requested to join the platform."),
            Map.entry("OFFERING_SUBMITTED_FOR_REVIEW", "%s was submitted for compliance review."),
            Map.entry("OFFERING_APPROVED", "%s was approved by compliance."),
            Map.entry("OFFERING_REJECTED", "%s was rejected by compliance."),
            Map.entry("OFFERING_TOKENIZATION_STARTED", "%s began on-chain tokenization."),
            Map.entry("OFFERING_CLOSED", "%s closed."),
            Map.entry("KYC_SUBMITTED", "An investor submitted KYC documentation."),
            Map.entry("KYC_VERIFIED", "An investor's KYC was verified."),
            Map.entry("KYC_REJECTED", "An investor's KYC was rejected."),
            Map.entry("DOCUMENT_UPLOADED", "A new document was uploaded."),
            Map.entry("INVESTOR_SELF_SIGNUP", "A new investor created an account."),
            Map.entry("WALLET_USER_PROVISIONED", "A new investor joined the platform with a wallet."));

    /** Falls back to this generic subject if the named entity can't be resolved (e.g. since deleted). */
    private static final Map<String, String> FALLBACK_SUBJECT = Map.of(
            "Organization", "A new organization",
            "Asset", "A property",
            "Offering", "An offering",
            "LegalStructure", "A legal structure");

    private final AuditApplicationService auditApplicationService;
    private final PlatformVisitRepository platformVisitRepository;
    private final OrganizationLookupPort organizationLookupPort;
    private final AssetLookupPort assetLookupPort;
    private final OfferingLookupPort offeringLookupPort;
    private final InvestmentLookupPort investmentLookupPort;
    private final DistributionLookupPort distributionLookupPort;
    private final LegalStructureLookupPort legalStructureLookupPort;

    public ActivityApplicationService(AuditApplicationService auditApplicationService,
            PlatformVisitRepository platformVisitRepository, OrganizationLookupPort organizationLookupPort,
            AssetLookupPort assetLookupPort, OfferingLookupPort offeringLookupPort,
            InvestmentLookupPort investmentLookupPort, DistributionLookupPort distributionLookupPort,
            LegalStructureLookupPort legalStructureLookupPort) {
        this.auditApplicationService = auditApplicationService;
        this.platformVisitRepository = platformVisitRepository;
        this.organizationLookupPort = organizationLookupPort;
        this.assetLookupPort = assetLookupPort;
        this.offeringLookupPort = offeringLookupPort;
        this.investmentLookupPort = investmentLookupPort;
        this.distributionLookupPort = distributionLookupPort;
        this.legalStructureLookupPort = legalStructureLookupPort;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedItem> getFeed(int limit) {
        return auditApplicationService.getAll().stream()
                .filter(entry -> NEWSWORTHY_ACTIONS.contains(entry.getAction()))
                .limit(limit)
                .map(this::toFeedItem)
                .toList();
    }

    /**
     * Total newsworthy-event count plus a breakdown by resource type (Offering, Investment,
     * Asset, ...) — same underlying set {@link #getFeed} draws from, just counted instead of
     * paginated, so "total" and the per-category numbers are always consistent with each other.
     */
    @Transactional(readOnly = true)
    public ActivityStatsResponse getStats() {
        List<AuditLogEntry> newsworthy = auditApplicationService.getAll().stream()
                .filter(entry -> NEWSWORTHY_ACTIONS.contains(entry.getAction()))
                .toList();
        Map<String, Long> countByCategory = newsworthy.stream()
                .collect(Collectors.groupingBy(AuditLogEntry::getResourceType, Collectors.counting()));
        return new ActivityStatsResponse(newsworthy.size(), countByCategory);
    }

    /**
     * Idempotent per user per day (see the {@code uq_platform_visit_user_date} constraint) —
     * called by the frontend once a session is active. Duplicate calls the same day are expected
     * (page reloads, multiple tabs) and silently no-op rather than erroring.
     */
    @Transactional
    public void recordVisit(TenantContext caller) {
        LocalDate today = LocalDate.now();
        if (platformVisitRepository.existsByUserIdAndVisitDate(caller.userId(), today)) {
            return;
        }
        String role = caller.hasRole("INVESTOR") ? "INVESTOR" : caller.roles().stream().findFirst().orElse("UNKNOWN");
        try {
            platformVisitRepository.save(new PlatformVisit(caller.userId(), role, today));
        } catch (DataIntegrityViolationException alreadyRecordedConcurrently) {
            // Another request for the same user/day won the race — nothing left to do.
        }
    }

    @Transactional(readOnly = true)
    public VisitStatsResponse getVisitStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        return new VisitStatsResponse(
                platformVisitRepository.countDistinctUsersSince(today),
                platformVisitRepository.countDistinctUsersSince(weekStart),
                platformVisitRepository.countDistinctUsersSince(monthStart),
                platformVisitRepository.countDistinctUsersSince(yearStart),
                platformVisitRepository.countDistinctUsersSinceForRole(today, "INVESTOR"),
                platformVisitRepository.countDistinctUsersSinceForRole(weekStart, "INVESTOR"),
                platformVisitRepository.countDistinctUsersSinceForRole(monthStart, "INVESTOR"),
                platformVisitRepository.countDistinctUsersSinceForRole(yearStart, "INVESTOR"));
    }

    private ActivityFeedItem toFeedItem(AuditLogEntry entry) {
        String message = switch (entry.getAction()) {
            // Unit counts here are safe to show (unlike an investment's dollar amount or investor
            // identity, which never appear in this platform-wide feed): an offering's total units
            // is already public on its own listing, and per-investment unit counts don't reveal
            // who invested or how much — see docs/critical-analysis.md.
            case "OFFERING_CREATED" -> offeringWithUnitsMessage(entry.getResourceId(),
                    "%s was created as a new investment offering of %,d units.");
            case "OFFERING_OPENED_FOR_INVESTMENT" -> offeringWithUnitsMessage(entry.getResourceId(),
                    "%s opened for investment with %,d units available.");
            case "INVESTMENT_INITIATED" -> investmentMessage(entry.getResourceId(),
                    "A new investment was initiated for %,d units of %s.");
            case "PAYMENT_CONFIRMED" -> investmentMessage(entry.getResourceId(),
                    "A payment was confirmed for %,d units of %s.");
            case "TOKEN_ISSUANCE_CONFIRMED" -> investmentMessage(entry.getResourceId(),
                    "%,d units were issued for a settled investment of %s.");
            case "DISTRIBUTION_CREATED" -> distributionMessage(entry.getResourceId(), "A new distribution was created for %s.");
            case "DISTRIBUTION_COMPLETED" -> distributionMessage(entry.getResourceId(), "A distribution was completed and paid out for %s.");
            case "LEGAL_STRUCTURE_ACTIVATED" -> legalStructureMessage(entry.getResourceId());
            default -> defaultMessage(entry);
        };
        return new ActivityFeedItem(entry.getId(), entry.getAction(), entry.getResourceType(), message, entry.getOccurredAt());
    }

    private String defaultMessage(AuditLogEntry entry) {
        String template = ACTION_MESSAGES.get(entry.getAction());
        if (template == null) {
            return humanize(entry.getAction());
        }
        if (template.contains("%s")) {
            return template.formatted(resolveSubject(entry.getResourceType(), entry.getResourceId()));
        }
        return template;
    }

    /** {@code template} takes the offering's name, then its total unit count, in that order. */
    private String offeringWithUnitsMessage(String offeringId, String template) {
        Optional<OfferingSnapshot> snapshot = offeringId == null ? Optional.empty() : offeringLookupPort.findSnapshot(offeringId);
        String name = snapshot.map(OfferingSnapshot::name).orElse(FALLBACK_SUBJECT.get("Offering"));
        long totalUnits = snapshot.map(OfferingSnapshot::totalUnits).orElse(0L);
        return template.formatted(name, totalUnits);
    }

    /** {@code template} takes the investment's unit count, then its offering's name, in that order. */
    private String investmentMessage(String investmentId, String template) {
        Optional<InvestmentSnapshot> investment = investmentId == null ? Optional.empty() : investmentLookupPort.findSnapshot(investmentId);
        long units = investment.map(InvestmentSnapshot::units).orElse(0L);
        String offeringName = investment.flatMap(i -> offeringLookupPort.findSnapshot(i.offeringId()))
                .map(OfferingSnapshot::name)
                .orElse(FALLBACK_SUBJECT.get("Offering"));
        return template.formatted(units, offeringName);
    }

    /** {@code template} takes the distribution's offering's name. No amount — see the class-level privacy note. */
    private String distributionMessage(String distributionId, String template) {
        String offeringName = (distributionId == null ? Optional.<String>empty() : distributionLookupPort.findOfferingId(distributionId))
                .flatMap(offeringLookupPort::findSnapshot)
                .map(OfferingSnapshot::name)
                .orElse(FALLBACK_SUBJECT.get("Offering"));
        return template.formatted(offeringName);
    }

    private String legalStructureMessage(String legalStructureId) {
        String name = (legalStructureId == null ? Optional.<String>empty() : legalStructureLookupPort.findDisplayName(legalStructureId))
                .orElse(FALLBACK_SUBJECT.get("LegalStructure"));
        return name + " was activated.";
    }

    /**
     * The real, non-confidential name of the entity a feed item is about (e.g. {@code Dubai
     * Business Tower SPV Units}) — or a generic fallback subject ({@link #FALLBACK_SUBJECT}) if
     * the entity can no longer be resolved.
     */
    private String resolveSubject(String resourceType, String resourceId) {
        Optional<String> name = resourceId == null ? Optional.empty() : switch (resourceType) {
            case "Organization" -> organizationLookupPort.findDisplayName(resourceId);
            case "Asset" -> assetLookupPort.findDisplayName(resourceId);
            case "Offering" -> offeringLookupPort.findSnapshot(resourceId).map(snapshot -> snapshot.name());
            default -> Optional.empty();
        };
        return name.orElseGet(() -> FALLBACK_SUBJECT.getOrDefault(resourceType, "Something"));
    }

    private String humanize(String action) {
        String lower = action.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1) + ".";
    }
}
