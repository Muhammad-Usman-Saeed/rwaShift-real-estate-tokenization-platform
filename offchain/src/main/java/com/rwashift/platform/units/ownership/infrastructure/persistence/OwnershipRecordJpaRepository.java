package com.rwashift.platform.units.ownership.infrastructure.persistence;

import com.rwashift.platform.units.ownership.domain.model.OwnershipRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OwnershipRecordJpaRepository extends JpaRepository<OwnershipRecord, String> {

    Optional<OwnershipRecord> findByTokenAddressAndWalletAddress(String tokenAddress, String walletAddress);

    List<OwnershipRecord> findByOfferingId(String offeringId);

    List<OwnershipRecord> findByInvestorId(String investorId);
}
