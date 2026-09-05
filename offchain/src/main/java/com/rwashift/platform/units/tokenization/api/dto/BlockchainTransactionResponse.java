package com.rwashift.platform.units.tokenization.api.dto;

import com.rwashift.platform.units.tokenization.domain.model.BlockchainTransaction;
import java.time.Instant;

public record BlockchainTransactionResponse(
        String id,
        String businessReferenceType,
        String businessReferenceId,
        String network,
        long chainId,
        String contractAddress,
        String method,
        String txHash,
        String status,
        Instant submittedAt,
        Instant confirmedAt,
        Long blockNumber,
        String failureReason,
        int retryCount
) {
    public static BlockchainTransactionResponse from(BlockchainTransaction tx) {
        return new BlockchainTransactionResponse(
                tx.getId(), tx.getBusinessReferenceType().name(), tx.getBusinessReferenceId(), tx.getNetwork(),
                tx.getChainId(), tx.getContractAddress(), tx.getMethod(), tx.getTxHash(), tx.getStatus().name(),
                tx.getSubmittedAt(), tx.getConfirmedAt(), tx.getBlockNumber(), tx.getFailureReason(), tx.getRetryCount());
    }
}
