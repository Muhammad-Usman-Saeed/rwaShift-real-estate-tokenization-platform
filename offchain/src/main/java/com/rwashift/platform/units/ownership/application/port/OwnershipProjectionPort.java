package com.rwashift.platform.units.ownership.application.port;

import java.math.BigInteger;

/**
 * Explicit cross-unit contract: Tokenization's event indexer calls this after processing a
 * {@code Transfer} event from an offering's token, so Ownership can update its read-model
 * projection. Ownership itself never touches Web3j — see {@code units.ownership} package
 * documentation: the blockchain remains the sole source of truth for actual balances; this
 * projection exists purely to make balances queryable without an RPC round trip per request.
 */
public interface OwnershipProjectionPort {

    void applyTransfer(String tokenAddress, String offeringId, String fromWallet, String toWallet,
            BigInteger amount, long blockNumber);
}
