package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransactionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface BlockchainTransactionJpaRepository extends JpaRepository<BlockchainTransaction, String> {

    List<BlockchainTransaction> findByStatusIn(List<BlockchainTransactionStatus> statuses);

    List<BlockchainTransaction> findByBusinessReferenceId(String businessReferenceId);
}
