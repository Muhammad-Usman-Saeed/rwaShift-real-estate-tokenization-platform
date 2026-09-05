package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.WalletLoginNonce;
import com.rwashift.platform.units.iam.domain.repository.WalletLoginNonceRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class WalletLoginNonceRepositoryAdapter implements WalletLoginNonceRepository {

    private final WalletLoginNonceJpaRepository jpaRepository;

    WalletLoginNonceRepositoryAdapter(WalletLoginNonceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WalletLoginNonce save(WalletLoginNonce nonce) {
        return jpaRepository.save(nonce);
    }

    @Override
    public Optional<WalletLoginNonce> findByWalletAddress(String walletAddress) {
        return jpaRepository.findById(walletAddress);
    }

    @Override
    public void delete(WalletLoginNonce nonce) {
        jpaRepository.delete(nonce);
    }
}
