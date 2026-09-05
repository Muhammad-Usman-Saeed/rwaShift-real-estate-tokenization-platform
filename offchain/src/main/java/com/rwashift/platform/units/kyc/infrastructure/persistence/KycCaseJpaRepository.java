package com.rwashift.platform.units.kyc.infrastructure.persistence;

import com.rwashift.platform.units.kyc.domain.model.KycCase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface KycCaseJpaRepository extends JpaRepository<KycCase, String> {

    Optional<KycCase> findByInvestorId(String investorId);
}
