package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransactionStatus;
import com.rwashift.platform.units.tokenization.domain.repository.BlockchainTransactionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class BlockchainTransactionRepositoryAdapter implements BlockchainTransactionRepository {

    private final BlockchainTransactionJpaRepository jpaRepository;

    BlockchainTransactionRepositoryAdapter(BlockchainTransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BlockchainTransaction save(BlockchainTransaction transaction) {
        return jpaRepository.save(transaction);
    }

    @Override
    public Optional<BlockchainTransaction> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<BlockchainTransaction> findByStatusIn(List<BlockchainTransactionStatus> statuses) {
        return jpaRepository.findByStatusIn(statuses);
    }

    @Override
    public List<BlockchainTransaction> findByBusinessReferenceId(String businessReferenceId) {
        return jpaRepository.findByBusinessReferenceId(businessReferenceId);
    }

    @Override
    public List<BlockchainTransaction> findAll() {
        return jpaRepository.findAll();
    }
}
