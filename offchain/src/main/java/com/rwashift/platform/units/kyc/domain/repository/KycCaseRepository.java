package com.rwashift.platform.units.kyc.domain.repository;

import com.rwashift.platform.units.kyc.domain.model.KycCase;
import java.util.List;
import java.util.Optional;

public interface KycCaseRepository {

    KycCase save(KycCase kycCase);

    Optional<KycCase> findById(String id);

    Optional<KycCase> findByInvestorId(String investorId);

    List<KycCase> findAll();
}
