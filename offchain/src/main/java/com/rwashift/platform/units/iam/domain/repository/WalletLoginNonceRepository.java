package com.rwashift.platform.units.iam.domain.repository;

import com.rwashift.platform.units.iam.domain.model.WalletLoginNonce;
import java.util.Optional;

public interface WalletLoginNonceRepository {

    WalletLoginNonce save(WalletLoginNonce nonce);

    Optional<WalletLoginNonce> findByWalletAddress(String walletAddress);

    void delete(WalletLoginNonce nonce);
}
