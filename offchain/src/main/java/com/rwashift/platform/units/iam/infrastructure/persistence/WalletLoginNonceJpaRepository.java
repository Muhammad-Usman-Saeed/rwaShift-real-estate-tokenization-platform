package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.WalletLoginNonce;
import org.springframework.data.jpa.repository.JpaRepository;

interface WalletLoginNonceJpaRepository extends JpaRepository<WalletLoginNonce, String> {
}
