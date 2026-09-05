package com.rwashift.platform.units.investment.application.port;

/**
 * Deliberately narrow — {@code offeringId} and {@code units} only, no investor id, no dollar
 * amount. Consumed by Activity's platform-wide (not tenant-scoped) news feed, where investor
 * identity and deal size must never appear; unit count alongside an offering's already-public name
 * is safe (see {@code ActivityApplicationService}), the same information a browsing investor could
 * already infer from the offering's own public "units available" figure.
 */
public record InvestmentSnapshot(String investmentId, String offeringId, long units) {
}
