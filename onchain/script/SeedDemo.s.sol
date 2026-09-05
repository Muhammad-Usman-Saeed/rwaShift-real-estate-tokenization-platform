// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { Script, console2 } from "forge-std/Script.sol";

import { IToken } from "@trex/token/IToken.sol";
import { SupplyLimitModule } from "@trex/compliance/modular/modules/SupplyLimitModule.sol";

import { RwaShiftTokenFactory } from "../src/core/RwaShiftTokenFactory.sol";
import { RwaShiftIdentityGateway } from "../src/identity/RwaShiftIdentityGateway.sol";

/// @notice Seeds a demo offering end-to-end against an already-deployed local platform
/// (see `DeployLocal.s.sol`), reading addresses from `deployments/local.json`, and walks through
/// the exact acceptance scenario from the product spec:
///
///   1. create offering "OFFERING-DBT-001" (20,000 units, $100/unit, mirroring "Dubai Business
///      Tower SPV Ltd." — the SPV/offering economics themselves live in Spring Boot, only the
///      opaque reference ID and ERC-3643 parameters go on-chain)
///   2. register Investor A's identity (eligible) — Investor B is deliberately left unregistered
///   3. issue 100 units to Investor A (simulating a confirmed $10,000 investment)
///   4. attempt A -> B transfer: MUST revert (B is not eligible)
///   5. register Investor B's identity (eligible)
///   6. repeat A -> B transfer: MUST succeed
///
/// Usage:
///   anvil                                                   # separate terminal
///   forge script script/DeployLocal.s.sol --rpc-url anvil --broadcast
///   forge script script/SeedDemo.s.sol --rpc-url anvil --broadcast -vvvv
contract SeedDemo is Script {
    string internal constant OFFERING_REF_ID = "OFFERING-DBT-001";
    uint256 internal constant OFFERING_UNITS = 20_000;
    uint256 internal constant INVESTOR_A_UNITS = 100;
    uint16 internal constant COUNTRY_AE = 784; // ISO-3166-1 numeric: United Arab Emirates

    function run() external {
        string memory json = vm.readFile("./deployments/local.json");

        uint256 deployerKey =
            vm.envOr("LOCAL_DEPLOYER_KEY", uint256(0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80));

        RwaShiftTokenFactory tokenFactory = RwaShiftTokenFactory(vm.parseJsonAddress(json, ".rwaShiftTokenFactory"));
        RwaShiftIdentityGateway identityGateway =
            RwaShiftIdentityGateway(vm.parseJsonAddress(json, ".rwaShiftIdentityGateway"));
        address supplyLimitModule = vm.parseJsonAddress(json, ".supplyLimitModule");

        // Demo wallets. In production these are investor/issuer-controlled keys; here we derive
        // deterministic local test keys so the script is fully self-contained.
        (address issuerAdmin,) = makeAddrAndKey("rwa-shift-demo-issuer-admin");
        (address tokenAgent, uint256 tokenAgentKey) = makeAddrAndKey("rwa-shift-demo-token-agent");
        (address investorA,) = makeAddrAndKey("rwa-shift-demo-investor-a");
        (address investorB,) = makeAddrAndKey("rwa-shift-demo-investor-b");

        // Fund the demo wallets so they can pay gas for their own broadcasted transactions
        // (mint by the token agent, transfer by Investor A).
        vm.startBroadcast(deployerKey);
        payable(tokenAgent).transfer(0.5 ether);
        payable(investorA).transfer(0.5 ether);
        vm.stopBroadcast();

        vm.startBroadcast(deployerKey);

        address[] memory identityAgents = new address[](1);
        identityAgents[0] = address(identityGateway);
        address[] memory tokenAgents = new address[](1);
        tokenAgents[0] = tokenAgent;
        address[] memory complianceModules = new address[](1);
        complianceModules[0] = supplyLimitModule;
        bytes[] memory complianceSettings = new bytes[](1);
        complianceSettings[0] = abi.encodeWithSelector(SupplyLimitModule.setSupplyLimit.selector, OFFERING_UNITS);

        address tokenAddr = tokenFactory.createOffering(
            OFFERING_REF_ID,
            "Dubai Business Tower SPV Units",
            "DBT",
            0,
            issuerAdmin,
            identityAgents,
            tokenAgents,
            complianceModules,
            complianceSettings
        );
        console2.log("Offering token deployed at", tokenAddr);
        vm.stopBroadcast();

        IToken token = IToken(tokenAddr);
        address identityRegistry = address(token.identityRegistry());

        // Step 2: register Investor A only. Investor B stays unverified for now.
        vm.startBroadcast(deployerKey);
        identityGateway.registerInvestorIdentity(identityRegistry, investorA, COUNTRY_AE, "investor-a-onchainid");
        vm.stopBroadcast();
        console2.log("Investor A registered as eligible; Investor B intentionally left unregistered");

        // Step 3: the token agent unpauses (T-REX tokens are deployed paused) and issues 100 units
        // to Investor A, simulating a confirmed $10,000 investment (unit price $100).
        vm.startBroadcast(tokenAgentKey);
        token.unpause();
        token.mint(investorA, INVESTOR_A_UNITS);
        vm.stopBroadcast();
        console2.log("Issued", INVESTOR_A_UNITS, "units to Investor A. Balance:", token.balanceOf(investorA));

        // Step 4: A -> B transfer must fail (B is not eligible yet). This is demonstrated with a
        // simulated `vm.prank` call (not a broadcast transaction) precisely because it is expected
        // to revert on-chain and a script should never submit a transaction it knows will fail.
        (, uint256 investorAKey) = makeAddrAndKey("rwa-shift-demo-investor-a");
        vm.prank(investorA);
        (bool ok,) = address(token).call(abi.encodeWithSelector(token.transfer.selector, investorB, 10));
        console2.log("Transfer to unverified Investor B reverted as expected:", !ok);

        // Step 5: register Investor B as eligible.
        vm.startBroadcast(deployerKey);
        identityGateway.registerInvestorIdentity(identityRegistry, investorB, COUNTRY_AE, "investor-b-onchainid");
        vm.stopBroadcast();
        console2.log("Investor B registered as eligible");

        // Step 6: A -> B transfer now succeeds.
        vm.startBroadcast(investorAKey);
        token.transfer(investorB, 10);
        vm.stopBroadcast();
        console2.log("Transfer to verified Investor B succeeded. Balances - A:", token.balanceOf(investorA));
        console2.log("Balances - B:", token.balanceOf(investorB));
    }
}
