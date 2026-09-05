package com.rwashift.platform.units.investor.domain.repository;

import com.rwashift.platform.units.investor.domain.model.Investor;
import java.util.List;
import java.util.Optional;

public interface InvestorRepository {

    Investor save(Investor investor);

    Optional<Investor> findById(String id);

    Optional<Investor> findByUserId(String userId);

    Optional<Investor> findByPrimaryWalletAddress(String walletAddress);

    List<Investor> findAll();
}
