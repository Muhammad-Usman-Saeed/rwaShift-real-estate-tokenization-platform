package com.rwashift.platform.units.ownership.domain.repository;

import com.rwashift.platform.units.ownership.domain.model.OwnershipRecord;
import java.util.List;
import java.util.Optional;

public interface OwnershipRecordRepository {

    OwnershipRecord save(OwnershipRecord record);

    Optional<OwnershipRecord> findByTokenAddressAndWalletAddress(String tokenAddress, String walletAddress);

    List<OwnershipRecord> findByOfferingId(String offeringId);

    List<OwnershipRecord> findByInvestorId(String investorId);
}
