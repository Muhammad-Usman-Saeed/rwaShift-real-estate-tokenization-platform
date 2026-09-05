package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainEvent;
import org.springframework.data.jpa.repository.JpaRepository;

interface BlockchainEventJpaRepository extends JpaRepository<BlockchainEvent, String> {

    boolean existsByTxHashAndLogIndex(String txHash, long logIndex);
}
