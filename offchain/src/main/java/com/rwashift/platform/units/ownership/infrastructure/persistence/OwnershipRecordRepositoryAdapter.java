package com.rwashift.platform.units.ownership.infrastructure.persistence;

import com.rwashift.platform.units.ownership.domain.model.OwnershipRecord;
import com.rwashift.platform.units.ownership.domain.repository.OwnershipRecordRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class OwnershipRecordRepositoryAdapter implements OwnershipRecordRepository {

    private final OwnershipRecordJpaRepository jpaRepository;

    OwnershipRecordRepositoryAdapter(OwnershipRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OwnershipRecord save(OwnershipRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public Optional<OwnershipRecord> findByTokenAddressAndWalletAddress(String tokenAddress, String walletAddress) {
        return jpaRepository.findByTokenAddressAndWalletAddress(tokenAddress, walletAddress);
    }

    @Override
    public List<OwnershipRecord> findByOfferingId(String offeringId) {
        return jpaRepository.findByOfferingId(offeringId);
    }

    @Override
    public List<OwnershipRecord> findByInvestorId(String investorId) {
        return jpaRepository.findByInvestorId(investorId);
    }
}
