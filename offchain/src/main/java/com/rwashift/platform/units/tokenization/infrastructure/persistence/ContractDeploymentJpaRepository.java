package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;
import com.rwashift.platform.units.tokenization.domain.model.ContractType;
import com.rwashift.platform.units.tokenization.domain.model.DeploymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContractDeploymentJpaRepository extends JpaRepository<ContractDeployment, String> {

    Optional<ContractDeployment> findByOfferingIdAndContractType(String offeringId, ContractType contractType);

    List<ContractDeployment> findByOfferingId(String offeringId);

    List<ContractDeployment> findByContractTypeAndStatus(ContractType contractType, DeploymentStatus status);
}
