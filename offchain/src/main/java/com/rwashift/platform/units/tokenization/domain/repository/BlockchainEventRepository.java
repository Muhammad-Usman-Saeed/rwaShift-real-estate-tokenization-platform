package com.rwashift.platform.units.tokenization.domain.repository;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainEvent;

public interface BlockchainEventRepository {

    BlockchainEvent save(BlockchainEvent event);

    boolean existsByTxHashAndLogIndex(String txHash, long logIndex);
}
