package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;
import com.rwashift.platform.units.tokenization.domain.model.ContractType;
import com.rwashift.platform.units.tokenization.domain.model.DeploymentStatus;
import com.rwashift.platform.units.tokenization.domain.repository.ContractDeploymentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ContractDeploymentRepositoryAdapter implements ContractDeploymentRepository {

    private final ContractDeploymentJpaRepository jpaRepository;

    ContractDeploymentRepositoryAdapter(ContractDeploymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ContractDeployment save(ContractDeployment deployment) {
        return jpaRepository.save(deployment);
    }

    @Override
    public Optional<ContractDeployment> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ContractDeployment> findByOfferingIdAndContractType(String offeringId, ContractType contractType) {
        return jpaRepository.findByOfferingIdAndContractType(offeringId, contractType);
    }

    @Override
    public List<ContractDeployment> findByOfferingId(String offeringId) {
        return jpaRepository.findByOfferingId(offeringId);
    }

    @Override
    public List<ContractDeployment> findByContractTypeAndStatus(ContractType contractType, DeploymentStatus status) {
        return jpaRepository.findByContractTypeAndStatus(contractType, status);
    }

    @Override
    public List<ContractDeployment> findAll() {
        return jpaRepository.findAll();
    }
}
