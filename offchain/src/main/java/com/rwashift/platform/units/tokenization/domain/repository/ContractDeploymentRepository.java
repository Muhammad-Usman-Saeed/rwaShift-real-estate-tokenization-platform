package com.rwashift.platform.units.tokenization.domain.repository;

import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;
import com.rwashift.platform.units.tokenization.domain.model.ContractType;
import java.util.List;
import java.util.Optional;

public interface ContractDeploymentRepository {

    ContractDeployment save(ContractDeployment deployment);

    Optional<ContractDeployment> findById(String id);

    Optional<ContractDeployment> findByOfferingIdAndContractType(String offeringId, ContractType contractType);

    List<ContractDeployment> findByOfferingId(String offeringId);

    List<ContractDeployment> findByContractTypeAndStatus(ContractType contractType,
            com.rwashift.platform.units.tokenization.domain.model.DeploymentStatus status);

    List<ContractDeployment> findAll();
}
