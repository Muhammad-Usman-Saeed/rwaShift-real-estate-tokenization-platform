package com.rwashift.platform.units.tokenization.application.port;

import java.math.BigInteger;

/**
 * Explicit cross-unit contract: Distribution optionally calls this once a distribution is
 * approved, to anchor an immutable on-chain reference (via the `onchain/` project's
 * `RwaShiftDistributionRegistry`) that off-chain records can be reconciled against. Recording
 * is a lightweight audit anchor, not the distribution mechanism itself — actual payouts remain
 * simulated in V1 (see {@code units.distribution}).
 */
public interface DistributionRecordingPort {

    void recordDistribution(String offeringId, String distributionRefId, long recordDateEpochSeconds,
            BigInteger totalAmountRef, byte[] metadataHash);
}
