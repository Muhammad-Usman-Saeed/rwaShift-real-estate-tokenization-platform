package com.rwashift.platform.units.ownership.api.dto;

import com.rwashift.platform.units.ownership.domain.model.OwnershipRecord;
import java.math.BigInteger;

/** {@code units}/{@code lastSyncedBlock} reflect the projection as of the indexer's last poll — see {@code OwnershipRecord}. */
public record OwnershipRecordResponse(
        String investorId,
        String offeringId,
        String walletAddress,
        String tokenAddress,
        BigInteger units,
        long lastSyncedBlock
) {
    public static OwnershipRecordResponse from(OwnershipRecord record) {
        return new OwnershipRecordResponse(record.getInvestorId(), record.getOfferingId(), record.getWalletAddress(),
                record.getTokenAddress(), record.getUnits(), record.getLastSyncedBlock());
    }
}
