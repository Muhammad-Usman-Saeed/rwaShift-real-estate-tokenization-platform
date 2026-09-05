package com.rwashift.platform.units.tokenization.domain.repository;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransactionStatus;
import java.util.List;
import java.util.Optional;

public interface BlockchainTransactionRepository {

    BlockchainTransaction save(BlockchainTransaction transaction);

    Optional<BlockchainTransaction> findById(String id);

    List<BlockchainTransaction> findByStatusIn(List<BlockchainTransactionStatus> statuses);

    List<BlockchainTransaction> findByBusinessReferenceId(String businessReferenceId);

    List<BlockchainTransaction> findAll();
}
