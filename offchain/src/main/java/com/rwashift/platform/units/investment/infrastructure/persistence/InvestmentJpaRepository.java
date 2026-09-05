package com.rwashift.platform.units.investment.infrastructure.persistence;

import com.rwashift.platform.units.investment.domain.model.Investment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface InvestmentJpaRepository extends JpaRepository<Investment, String> {

    Optional<Investment> findByIdempotencyKey(String idempotencyKey);

    List<Investment> findByInvestorId(String investorId);

    List<Investment> findByOrganizationId(String organizationId);

    List<Investment> findByOfferingId(String offeringId);
}
