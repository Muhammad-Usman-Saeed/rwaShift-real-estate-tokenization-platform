// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { Script, console2 } from "forge-std/Script.sol";
import { PlatformDeploy } from "./lib/PlatformDeploy.sol";
import { MockUSDC } from "../src/core/MockUSDC.sol";
import { RwaShiftPaymentRouter } from "../src/core/RwaShiftPaymentRouter.sol";

/// @notice Deploys the full RWA Shift on-chain platform to a local Anvil node, using Anvil's
/// default account #0 as PLATFORM_ADMIN. Writes deployment addresses to `deployments/local.json`.
///
/// Usage:
///   anvil
///   forge script script/DeployLocal.s.sol:DeployLocal --rpc-url anvil --broadcast
contract DeployLocal is Script {
    // Anvil default account #0 private key (well-known, local-only — never used on a real network).
    uint256 internal constant ANVIL_DEPLOYER_KEY = 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80;

    // Anvil default account #3 — a plain receiving address for the crypto-payment flow (see
    // rwashift.blockchain.payment-collection-address). Deliberately not one of the accounts
    // already used elsewhere in the demo (deployer/agent is #0, demo Investor A/B are #1/#2) so
    // incoming payments are visibly separate from those roles. Receive-only: the backend never
    // needs (and is never given) this address's private key.
    address internal constant PAYMENT_COLLECTION_ADDRESS = 0x90F79bf6EB2c4f870365E785982E1f101E93b906;

    // Anvil default accounts #1/#2 — must match DemoDataSeeder's Investor A/B wallet addresses.
    address internal constant DEMO_INVESTOR_A = 0x70997970C51812dc3A010C7d01b50e0d17dc79C8;
    address internal constant DEMO_INVESTOR_B = 0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC;

    function run() external returns (PlatformDeploy.Deployment memory d) {
        uint256 deployerKey = vm.envOr("LOCAL_DEPLOYER_KEY", ANVIL_DEPLOYER_KEY);
        address platformAdmin = vm.addr(deployerKey);

        vm.startBroadcast(deployerKey);
        d = PlatformDeploy.run(platformAdmin);

        MockUSDC usdc = new MockUSDC();
        // 1,000,000 USDC (6 decimals) each — comfortably covers demo investment amounts.
        usdc.mint(DEMO_INVESTOR_A, 1_000_000 * 10 ** 6);
        usdc.mint(DEMO_INVESTOR_B, 1_000_000 * 10 ** 6);

        RwaShiftPaymentRouter paymentRouter = new RwaShiftPaymentRouter(address(usdc), PAYMENT_COLLECTION_ADDRESS);
        vm.stopBroadcast();

        console2.log("PLATFORM_ADMIN               ", platformAdmin);
        console2.log("Token implementation          ", d.tokenImplementation);
        console2.log("ClaimTopicsRegistry impl       ", d.ctrImplementation);
        console2.log("TrustedIssuersRegistry impl    ", d.tirImplementation);
        console2.log("IdentityRegistryStorage impl   ", d.irsImplementation);
        console2.log("IdentityRegistry impl          ", d.irImplementation);
        console2.log("ModularCompliance impl         ", d.mcImplementation);
        console2.log("TREXImplementationAuthority     ", d.trexImplementationAuthority);
        console2.log("OnchainID Identity impl         ", d.identityImplementation);
        console2.log("OnchainID ImplementationAuthority", d.identityImplementationAuthority);
        console2.log("OnchainID IdFactory              ", d.idFactory);
        console2.log("TREXFactory                      ", d.trexFactory);
        console2.log("RwaShiftTokenFactory             ", d.rwaShiftTokenFactory);
        console2.log("RwaShiftIdentityGateway          ", d.rwaShiftIdentityGateway);
        console2.log("RwaShiftDistributionRegistry     ", d.rwaShiftDistributionRegistry);
        console2.log("SupplyLimitModule (shared)       ", d.supplyLimitModule);
        console2.log("MockUSDC                         ", address(usdc));
        console2.log("Payment collection address       ", PAYMENT_COLLECTION_ADDRESS);
        console2.log("RwaShiftPaymentRouter            ", address(paymentRouter));

        _writeDeploymentJson(d, platformAdmin, address(usdc), address(paymentRouter));
    }

    function _writeDeploymentJson(PlatformDeploy.Deployment memory d, address platformAdmin, address usdcAddress, address paymentRouterAddress)
        internal
    {
        string memory json = "deployment";
        vm.serializeUint(json, "chainId", block.chainid);
        vm.serializeAddress(json, "platformAdmin", platformAdmin);
        vm.serializeAddress(json, "tokenImplementation", d.tokenImplementation);
        vm.serializeAddress(json, "ctrImplementation", d.ctrImplementation);
        vm.serializeAddress(json, "tirImplementation", d.tirImplementation);
        vm.serializeAddress(json, "irsImplementation", d.irsImplementation);
        vm.serializeAddress(json, "irImplementation", d.irImplementation);
        vm.serializeAddress(json, "mcImplementation", d.mcImplementation);
        vm.serializeAddress(json, "trexImplementationAuthority", d.trexImplementationAuthority);
        vm.serializeAddress(json, "identityImplementation", d.identityImplementation);
        vm.serializeAddress(json, "identityImplementationAuthority", d.identityImplementationAuthority);
        vm.serializeAddress(json, "idFactory", d.idFactory);
        vm.serializeAddress(json, "trexFactory", d.trexFactory);
        vm.serializeAddress(json, "rwaShiftTokenFactory", d.rwaShiftTokenFactory);
        vm.serializeAddress(json, "rwaShiftIdentityGateway", d.rwaShiftIdentityGateway);
        vm.serializeAddress(json, "rwaShiftDistributionRegistry", d.rwaShiftDistributionRegistry);
        vm.serializeAddress(json, "supplyLimitModule", d.supplyLimitModule);
        vm.serializeAddress(json, "usdcTokenAddress", usdcAddress);
        vm.serializeAddress(json, "paymentCollectionAddress", PAYMENT_COLLECTION_ADDRESS);
        string memory finalJson = vm.serializeAddress(json, "paymentRouterAddress", paymentRouterAddress);

        vm.writeJson(finalJson, "./deployments/local.json");
        console2.log("Wrote deployments/local.json");
    }
}
