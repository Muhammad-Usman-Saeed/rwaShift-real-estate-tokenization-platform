package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainEvent;
import com.rwashift.platform.units.tokenization.domain.repository.BlockchainEventRepository;
import org.springframework.stereotype.Repository;

@Repository
class BlockchainEventRepositoryAdapter implements BlockchainEventRepository {

    private final BlockchainEventJpaRepository jpaRepository;

    BlockchainEventRepositoryAdapter(BlockchainEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BlockchainEvent save(BlockchainEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public boolean existsByTxHashAndLogIndex(String txHash, long logIndex) {
        return jpaRepository.existsByTxHashAndLogIndex(txHash, logIndex);
    }
}
