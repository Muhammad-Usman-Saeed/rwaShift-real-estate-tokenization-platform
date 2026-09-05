package com.rwashift.platform.units.tokenization.infrastructure.integration;

import com.rwashift.platform.units.tokenization.infrastructure.configuration.BlockchainProperties;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

/**
 * The single adapter that actually encodes and submits ERC-3643 platform contract calls. Every
 * method here corresponds 1:1 to a function on one of the contracts in the separate `onchain/`
 * Foundry project (`RwaShiftTokenFactory`, `RwaShiftIdentityGateway`, T-REX's `Token`) — no
 * business logic, only ABI encoding + submission. Consumed only by
 * {@code TokenizationApplicationService} and {@code BlockchainTransactionManager}, both in this
 * same composite unit; every other unit in the platform is unaware Web3j exists.
 */
@Component
public class Web3jContractGateway {

    private final Web3j web3j;
    private final TransactionManager transactionManager;
    private final Credentials agentCredentials;
    private final BlockchainProperties properties;

    Web3jContractGateway(Web3j web3j, TransactionManager transactionManager, Credentials agentCredentials,
            BlockchainProperties properties) {
        this.web3j = web3j;
        this.transactionManager = transactionManager;
        this.agentCredentials = agentCredentials;
        this.properties = properties;
    }

    /** RwaShiftTokenFactory.createOffering(string,string,string,uint8,address,address[],address[],address[],bytes[]) */
    public String createOffering(String offeringRefId, String name, String symbol, int decimals, String issuerAdmin,
            List<String> identityAgents, List<String> tokenAgents) {
        Function function = new Function(
                "createOffering",
                List.of(
                        new Utf8String(offeringRefId),
                        new Utf8String(name),
                        new Utf8String(symbol),
                        new Uint8(BigInteger.valueOf(decimals)),
                        new Address(issuerAdmin),
                        addressArray(identityAgents),
                        addressArray(tokenAgents),
                        new DynamicArray<>(Address.class, List.of()),
                        new DynamicArray<>(DynamicBytes.class, List.of())),
                List.of());
        return sendTransaction(properties.getTokenFactoryAddress(), function);
    }

    /** RwaShiftIdentityGateway.registerInvestorIdentity(address,address,uint16,string) */
    public String registerInvestorIdentity(String identityRegistryAddress, String walletAddress, int countryCode, String salt) {
        Function function = new Function(
                "registerInvestorIdentity",
                List.of(new Address(identityRegistryAddress), new Address(walletAddress),
                        new Uint16(BigInteger.valueOf(countryCode)), new Utf8String(salt)),
                List.of());
        return sendTransaction(properties.getIdentityGatewayAddress(), function);
    }

    /**
     * Token.unpause() — T-REX tokens deploy paused; the token agent must unpause before the
     * first mint. {@code signer} is the owning organization's own wallet (see {@link
     * com.rwashift.platform.units.tokenization.application.service.OrganizationWalletProvisioningService}),
     * not the platform's shared agent key — see docs/critical-analysis.md.
     */
    public String unpauseToken(String tokenAddress, Credentials signer) {
        Function function = new Function("unpause", List.of(), List.of());
        return sendTransactionAs(signer, tokenAddress, function);
    }

    /** Token.mint(address,uint256) — signed by the owning organization's own wallet, not the shared platform agent. */
    public String mint(String tokenAddress, String walletAddress, long units, Credentials signer) {
        Function function = new Function(
                "mint",
                List.of(new Address(walletAddress), new Uint256(BigInteger.valueOf(units))),
                List.of());
        return sendTransactionAs(signer, tokenAddress, function);
    }

    /** RwaShiftTokenFactory.tokenForOffering(string) — view call, used once deployment confirms. */
    public String tokenForOffering(String offeringRefId) {
        Function function = new Function(
                "tokenForOffering",
                List.of(new Utf8String(offeringRefId)),
                List.of(new TypeReference<Address>() {
                }));
        return ethCall(properties.getTokenFactoryAddress(), function).get(0).getValue().toString();
    }

    /** Token.identityRegistry() — view call, resolves the per-offering IdentityRegistry address. */
    public String identityRegistryOf(String tokenAddress) {
        Function function = new Function(
                "identityRegistry",
                List.of(),
                List.of(new TypeReference<Address>() {
                }));
        return ethCall(tokenAddress, function).get(0).getValue().toString();
    }

    /** Token.balanceOf(address) — view call, used by Ownership reconciliation and reporting. */
    public BigInteger balanceOf(String tokenAddress, String walletAddress) {
        Function function = new Function(
                "balanceOf",
                List.of(new Address(walletAddress)),
                List.of(new TypeReference<Uint256>() {
                }));
        return (BigInteger) ethCall(tokenAddress, function).get(0).getValue();
    }

    /** Token.burn(address,uint256) — signed by the owning organization's own wallet, not the shared platform agent. */
    public String burn(String tokenAddress, String walletAddress, long units, Credentials signer) {
        Function function = new Function(
                "burn",
                List.of(new Address(walletAddress), new Uint256(BigInteger.valueOf(units))),
                List.of());
        return sendTransactionAs(signer, tokenAddress, function);
    }

    /** Token.setAddressFrozen(address,bool) — signed by the owning organization's own wallet, not the shared platform agent. */
    public String setAddressFrozen(String tokenAddress, String walletAddress, boolean frozen, Credentials signer) {
        Function function = new Function(
                "setAddressFrozen",
                List.of(new Address(walletAddress), new Bool(frozen)),
                List.of());
        return sendTransactionAs(signer, tokenAddress, function);
    }

    /** RwaShiftIdentityGateway.updateInvestorCountry(address,address,uint16) */
    public String updateInvestorCountry(String identityRegistryAddress, String walletAddress, int countryCode) {
        Function function = new Function(
                "updateInvestorCountry",
                List.of(new Address(identityRegistryAddress), new Address(walletAddress),
                        new Uint16(BigInteger.valueOf(countryCode))),
                List.of());
        return sendTransaction(properties.getIdentityGatewayAddress(), function);
    }

    /** RwaShiftDistributionRegistry.recordDistribution(address,string,uint64,uint256,bytes32) */
    public String recordDistribution(String tokenAddress, String distributionRefId, long recordDate,
            BigInteger totalAmountRef, byte[] metadataHash) {
        Function function = new Function(
                "recordDistribution",
                List.of(new Address(tokenAddress), new Utf8String(distributionRefId),
                        new Uint64(BigInteger.valueOf(recordDate)), new Uint256(totalAmountRef),
                        new Bytes32(metadataHash)),
                List.of());
        return sendTransaction(properties.getDistributionRegistryAddress(), function);
    }

    private static final org.web3j.abi.datatypes.Event TRANSFER_EVENT = new org.web3j.abi.datatypes.Event(
            "Transfer",
            List.of(new TypeReference<Address>(true) {
                    }, new TypeReference<Address>(true) {
                    },
                    new TypeReference<Uint256>(false) {
                    }));

    /** ERC-20/ERC-3643 {@code Transfer(address,address,uint256)} logs for one contract, inclusive block range. */
    public List<org.web3j.protocol.core.methods.response.EthLog.LogResult> getTransferLogs(String contractAddress,
            long fromBlock, long toBlock) {
        org.web3j.protocol.core.methods.request.EthFilter filter = new org.web3j.protocol.core.methods.request.EthFilter(
                new org.web3j.protocol.core.DefaultBlockParameterNumber(fromBlock),
                new org.web3j.protocol.core.DefaultBlockParameterNumber(toBlock),
                contractAddress);
        filter.addSingleTopic(org.web3j.abi.EventEncoder.encode(TRANSFER_EVENT));
        try {
            return web3j.ethGetLogs(filter).send().getLogs();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch Transfer logs for " + contractAddress, e);
        }
    }

    public String decodeTransferFrom(org.web3j.protocol.core.methods.response.Log log) {
        return "0x" + log.getTopics().get(1).substring(26);
    }

    public String decodeTransferTo(org.web3j.protocol.core.methods.response.Log log) {
        return "0x" + log.getTopics().get(2).substring(26);
    }

    public BigInteger decodeTransferAmount(org.web3j.protocol.core.methods.response.Log log) {
        return new BigInteger(log.getData().substring(2), 16);
    }

    private static final org.web3j.abi.datatypes.Event PAYMENT_RECEIVED_EVENT = new org.web3j.abi.datatypes.Event(
            "PaymentReceived",
            List.of(new TypeReference<Address>(true) {
                    },
                    new TypeReference<Utf8String>(false) {
                    },
                    new TypeReference<Uint256>(false) {
                    }));

    /** {@code RwaShiftPaymentRouter.PaymentReceived(address indexed payer, string investmentId, uint256 amount)} logs. */
    public List<org.web3j.protocol.core.methods.response.EthLog.LogResult> getPaymentReceivedLogs(String routerAddress,
            long fromBlock, long toBlock) {
        org.web3j.protocol.core.methods.request.EthFilter filter = new org.web3j.protocol.core.methods.request.EthFilter(
                new org.web3j.protocol.core.DefaultBlockParameterNumber(fromBlock),
                new org.web3j.protocol.core.DefaultBlockParameterNumber(toBlock),
                routerAddress);
        filter.addSingleTopic(org.web3j.abi.EventEncoder.encode(PAYMENT_RECEIVED_EVENT));
        try {
            return web3j.ethGetLogs(filter).send().getLogs();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch PaymentReceived logs for " + routerAddress, e);
        }
    }

    public String decodePaymentReceivedPayer(org.web3j.protocol.core.methods.response.Log log) {
        return "0x" + log.getTopics().get(1).substring(26);
    }

    /** The non-indexed {@code (investmentId, amount)} pair — decoded together since both live in the same ABI-encoded {@code data} blob. */
    public java.util.Map.Entry<String, BigInteger> decodePaymentReceivedData(org.web3j.protocol.core.methods.response.Log log) {
        List<Type> decoded = FunctionReturnDecoder.decode(log.getData(), PAYMENT_RECEIVED_EVENT.getNonIndexedParameters());
        String investmentId = ((Utf8String) decoded.get(0)).getValue();
        BigInteger amount = ((Uint256) decoded.get(1)).getValue();
        return java.util.Map.entry(investmentId, amount);
    }

    /** Native balance (wei) of any address — used to show whether an organization's wallet needs funding. */
    public BigInteger getBalance(String address) {
        try {
            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch balance for " + address, e);
        }
    }

    public long currentBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber().longValueExact();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read current block number", e);
        }
    }

    /**
     * {@code topic0 -> human label} for the platform's own (non-Transfer) contract events —
     * used only to describe the pre-login "chain activity" feed (see {@code
     * NetworkStatusApplicationService}), never to drive business logic. Signatures are hashed
     * from the exact event declarations in {@code onchain/src/core/RwaShiftTokenFactory.sol},
     * {@code onchain/src/identity/RwaShiftIdentityGateway.sol}, and {@code
     * onchain/src/core/RwaShiftDistributionRegistry.sol} — keep in sync if those ever change.
     */
    private static final Map<String, String> PLATFORM_EVENT_NAMES = Map.of(
            Hash.sha3String("OfferingTokenCreated(string,address,address,string,string,uint8,address,address)"),
            "Offering token created",
            Hash.sha3String("InvestorIdentityRegistered(address,address,address,uint16)"),
            "Investor identity registered",
            Hash.sha3String("InvestorIdentityUpdated(address,address,uint16)"),
            "Investor identity updated",
            Hash.sha3String("InvestorIdentityRevoked(address,address)"),
            "Investor identity revoked",
            Hash.sha3String("DistributionRecorded(address,string,uint64,uint256,bytes32,address)"),
            "Distribution recorded");

    /**
     * Raw logs from the platform's own contracts (Token Factory, Identity Gateway, Distribution
     * Registry) — deliberately excludes per-offering token contracts (that's {@link
     * #getTransferLogs}'s job) since those addresses aren't known without an offering context,
     * whereas these three are fixed at deployment time. Returns an empty list rather than
     * erroring when none of the three addresses are configured yet (e.g. tests).
     */
    public List<Log> getRecentPlatformLogs(long fromBlock, long toBlock) {
        List<String> addresses = List.of(properties.getTokenFactoryAddress(), properties.getIdentityGatewayAddress(),
                        properties.getDistributionRegistryAddress()).stream()
                .filter(address -> address != null && !address.isBlank())
                .toList();
        if (addresses.isEmpty()) {
            return List.of();
        }
        EthFilter filter = new EthFilter(
                new DefaultBlockParameterNumber(fromBlock), new DefaultBlockParameterNumber(toBlock), addresses);
        try {
            return web3j.ethGetLogs(filter).send().getLogs().stream().map(result -> (Log) result.get()).toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch platform logs", e);
        }
    }

    /** Human label for a platform event log, resolved by {@code topic0}. Never "Transfer" — see {@link #getRecentPlatformLogs}. */
    public String describePlatformEvent(Log log) {
        if (log.getTopics().isEmpty()) {
            return "Platform event";
        }
        return PLATFORM_EVENT_NAMES.getOrDefault(log.getTopics().get(0), "Platform event");
    }

    /** Human label for which platform contract emitted a log, resolved by address. */
    public String contractLabel(String address) {
        if (address != null && address.equalsIgnoreCase(properties.getTokenFactoryAddress())) {
            return "Token Factory";
        }
        if (address != null && address.equalsIgnoreCase(properties.getIdentityGatewayAddress())) {
            return "Identity Gateway";
        }
        if (address != null && address.equalsIgnoreCase(properties.getDistributionRegistryAddress())) {
            return "Distribution Registry";
        }
        return "Platform Contract";
    }

    /** Wall-clock time a block was mined, for computing average block time / relative-age display. */
    public Instant blockTimestamp(long blockNumber) {
        try {
            var block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber), false).send().getBlock();
            return block == null ? null : Instant.ofEpochSecond(block.getTimestamp().longValueExact());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch block " + blockNumber, e);
        }
    }

    /** Returns {@code null} while the receipt is not yet available (still pending). */
    public TransactionReceipt fetchReceipt(String txHash) {
        try {
            return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt().orElse(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch receipt for " + txHash, e);
        }
    }

    private List<Type> ethCall(String contractAddress, Function function) {
        String encodedFunction = FunctionEncoder.encode(function);
        try {
            Transaction transaction = Transaction.createEthCallTransaction(
                    agentCredentials.getAddress(), contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            if (response.hasError()) {
                throw new IllegalStateException("Node rejected eth_call: " + response.getError().getMessage());
            }
            return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed eth_call to " + contractAddress, e);
        }
    }

    /**
     * Submits and returns immediately with the transaction hash — deliberately does NOT wait
     * for a receipt here. Confirmation is {@code BlockchainTransactionManager}'s job, polling
     * asynchronously, so submitting a transaction never blocks an API request on block time.
     * Signed by the platform's single shared agent key — only for calls that are legitimately
     * platform-level (offering-suite creation, investor identity registration/compliance,
     * distribution record-keeping). Anything that moves or controls an organization's own token
     * supply goes through {@link #sendTransactionAs} instead.
     */
    private String sendTransaction(String contractAddress, Function function) {
        String encodedFunction = FunctionEncoder.encode(function);
        try {
            EthSendTransaction response = transactionManager.sendTransaction(
                    gasPrice(), estimateGasWithMargin(agentCredentials.getAddress(), contractAddress, encodedFunction),
                    contractAddress, encodedFunction, BigInteger.ZERO);
            if (response.hasError()) {
                throw new IllegalStateException("Node rejected transaction: " + response.getError().getMessage());
            }
            return response.getTransactionHash();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit transaction to " + contractAddress, e);
        }
    }

    /**
     * Same as {@link #sendTransaction}, but signed by {@code signer} — an organization's own
     * on-chain wallet — rather than the platform's shared agent key. Builds a one-off {@link
     * RawTransactionManager} per call instead of caching one per organization, so a decrypted
     * private key never lingers in memory longer than a single transaction submission.
     */
    private String sendTransactionAs(Credentials signer, String contractAddress, Function function) {
        String encodedFunction = FunctionEncoder.encode(function);
        try {
            TransactionManager orgTransactionManager = new RawTransactionManager(web3j, signer, properties.getChainId(),
                    new PollingTransactionReceiptProcessor(web3j, properties.getPollIntervalMs(), 40));
            EthSendTransaction response = orgTransactionManager.sendTransaction(
                    gasPrice(), estimateGasWithMargin(signer.getAddress(), contractAddress, encodedFunction),
                    contractAddress, encodedFunction, BigInteger.ZERO);
            if (response.hasError()) {
                throw new IllegalStateException("Node rejected transaction: " + response.getError().getMessage());
            }
            return response.getTransactionHash();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit transaction to " + contractAddress, e);
        }
    }

    /**
     * A single fixed gas limit doesn't fit every call this gateway makes — a simple
     * {@code mint}/{@code transfer} costs a small fraction of what {@code createOffering} does
     * (it deploys an entire T-REX token suite: several proxy contracts plus wiring calls), so a
     * limit sized for the cheap calls silently runs out of gas — mined but reverted, with no
     * revert reason — on the expensive one. Estimating per-call and padding by 30% absorbs both
     * legitimate state-dependent variance and the small amount {@code eth_estimateGas} itself
     * tends to undercount versus actual execution.
     */
    private BigInteger estimateGasWithMargin(String from, String contractAddress, String encodedFunction) {
        try {
            Transaction transaction = Transaction.createFunctionCallTransaction(
                    from, null, null, null, contractAddress, encodedFunction);
            var response = web3j.ethEstimateGas(transaction).send();
            if (response.hasError()) {
                throw new IllegalStateException("Node rejected gas estimate: " + response.getError().getMessage());
            }
            BigInteger estimate = response.getAmountUsed();
            return estimate.multiply(BigInteger.valueOf(130)).divide(BigInteger.valueOf(100));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to estimate gas for " + contractAddress, e);
        }
    }

    private BigInteger gasPrice() {
        try {
            return web3j.ethGasPrice().send().getGasPrice();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch gas price", e);
        }
    }

    private static DynamicArray<Address> addressArray(List<String> addresses) {
        return new DynamicArray<>(Address.class, addresses.stream().map(Address::new).toList());
    }
}
