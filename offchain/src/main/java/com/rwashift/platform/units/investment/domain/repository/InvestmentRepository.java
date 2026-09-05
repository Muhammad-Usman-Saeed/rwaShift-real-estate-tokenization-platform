package com.rwashift.platform.units.investment.domain.repository;

import com.rwashift.platform.units.investment.domain.model.Investment;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository {

    Investment save(Investment investment);

    Optional<Investment> findById(String id);

    Optional<Investment> findByIdempotencyKey(String idempotencyKey);

    List<Investment> findByInvestorId(String investorId);

    List<Investment> findByOrganizationId(String organizationId);

    List<Investment> findByOfferingId(String offeringId);

    List<Investment> findAll();
}
