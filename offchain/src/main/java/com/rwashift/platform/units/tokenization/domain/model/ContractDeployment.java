package com.rwashift.platform.units.tokenization.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Records one on-chain contract belonging to one offering's ERC-3643 suite (the T-REX token
 * itself, its IdentityRegistry, or its ModularCompliance). This is metadata ONLY — the contract
 * bytecode/ABI/business logic lives entirely in the separate `onchain/` Foundry project;
 * Tokenization just remembers where each offering's contracts are deployed and whether that
 * deployment is confirmed.
 */
@Entity
@Table(name = "contract_deployment")
public class ContractDeployment extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "offering_id", nullable = false, length = 26)
    private String offeringId;

    @Column(name = "network", nullable = false, length = 32)
    private String network;

    @Column(name = "chain_id", nullable = false)
    private long chainId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 32)
    private ContractType contractType;

    @Column(name = "contract_address", length = 42)
    private String contractAddress;

    @Column(name = "deployment_tx_hash", length = 66)
    private String deploymentTxHash;

    @Column(name = "deployment_block")
    private Long deploymentBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DeploymentStatus status = DeploymentStatus.PENDING;

    protected ContractDeployment() {
    }

    public ContractDeployment(String offeringId, String network, long chainId, ContractType contractType,
            String deploymentTxHash) {
        this.offeringId = offeringId;
        this.network = network;
        this.chainId = chainId;
        this.contractType = contractType;
        this.deploymentTxHash = deploymentTxHash;
    }

    public void confirm(String contractAddress, long blockNumber) {
        this.contractAddress = contractAddress;
        this.deploymentBlock = blockNumber;
        this.status = DeploymentStatus.CONFIRMED;
    }

    public void fail() {
        this.status = DeploymentStatus.FAILED;
    }

    public String getId() {
        return id;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public String getNetwork() {
        return network;
    }

    public long getChainId() {
        return chainId;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getDeploymentTxHash() {
        return deploymentTxHash;
    }

    public Long getDeploymentBlock() {
        return deploymentBlock;
    }

    public DeploymentStatus getStatus() {
        return status;
    }
}
