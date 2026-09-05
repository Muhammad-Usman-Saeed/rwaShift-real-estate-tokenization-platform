package com.rwashift.platform.units.tokenization.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rwashift.blockchain")
public class BlockchainProperties {

    private String network;
    private long chainId;
    private String rpcUrl;
    private int confirmationsRequired = 1;
    private String tokenFactoryAddress;
    private String identityGatewayAddress;
    private String distributionRegistryAddress;
    private String agentPrivateKey;
    private long pollIntervalMs = 4000;
    private String usdcTokenAddress;
    private String paymentCollectionAddress;
    private String paymentRouterAddress;
    private String orgWalletMasterKey;

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public int getConfirmationsRequired() {
        return confirmationsRequired;
    }

    public void setConfirmationsRequired(int confirmationsRequired) {
        this.confirmationsRequired = confirmationsRequired;
    }

    public String getTokenFactoryAddress() {
        return tokenFactoryAddress;
    }

    public void setTokenFactoryAddress(String tokenFactoryAddress) {
        this.tokenFactoryAddress = tokenFactoryAddress;
    }

    public String getIdentityGatewayAddress() {
        return identityGatewayAddress;
    }

    public void setIdentityGatewayAddress(String identityGatewayAddress) {
        this.identityGatewayAddress = identityGatewayAddress;
    }

    public String getDistributionRegistryAddress() {
        return distributionRegistryAddress;
    }

    public void setDistributionRegistryAddress(String distributionRegistryAddress) {
        this.distributionRegistryAddress = distributionRegistryAddress;
    }

    public String getAgentPrivateKey() {
        return agentPrivateKey;
    }

    public void setAgentPrivateKey(String agentPrivateKey) {
        this.agentPrivateKey = agentPrivateKey;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public String getUsdcTokenAddress() {
        return usdcTokenAddress;
    }

    public void setUsdcTokenAddress(String usdcTokenAddress) {
        this.usdcTokenAddress = usdcTokenAddress;
    }

    public String getPaymentCollectionAddress() {
        return paymentCollectionAddress;
    }

    public void setPaymentCollectionAddress(String paymentCollectionAddress) {
        this.paymentCollectionAddress = paymentCollectionAddress;
    }

    public String getPaymentRouterAddress() {
        return paymentRouterAddress;
    }

    public void setPaymentRouterAddress(String paymentRouterAddress) {
        this.paymentRouterAddress = paymentRouterAddress;
    }

    public String getOrgWalletMasterKey() {
        return orgWalletMasterKey;
    }

    public void setOrgWalletMasterKey(String orgWalletMasterKey) {
        this.orgWalletMasterKey = orgWalletMasterKey;
    }
}
