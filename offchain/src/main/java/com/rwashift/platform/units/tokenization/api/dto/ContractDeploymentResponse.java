package com.rwashift.platform.units.tokenization.api.dto;

import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;

public record ContractDeploymentResponse(
        String offeringId,
        String network,
        long chainId,
        String contractType,
        String contractAddress,
        String deploymentTxHash,
        Long deploymentBlock,
        String status
) {
    public static ContractDeploymentResponse from(ContractDeployment deployment) {
        return new ContractDeploymentResponse(
                deployment.getOfferingId(), deployment.getNetwork(), deployment.getChainId(),
                deployment.getContractType().name(), deployment.getContractAddress(), deployment.getDeploymentTxHash(),
                deployment.getDeploymentBlock(), deployment.getStatus().name());
    }
}
