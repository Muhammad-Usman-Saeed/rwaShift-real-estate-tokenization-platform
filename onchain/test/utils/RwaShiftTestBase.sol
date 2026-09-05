// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { Test } from "forge-std/Test.sol";

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";
import { SupplyLimitModule } from "@trex/compliance/modular/modules/SupplyLimitModule.sol";

import { PlatformDeploy } from "../../script/lib/PlatformDeploy.sol";
import { RwaShiftTokenFactory } from "../../src/core/RwaShiftTokenFactory.sol";
import { RwaShiftIdentityGateway } from "../../src/identity/RwaShiftIdentityGateway.sol";
import { RwaShiftDistributionRegistry } from "../../src/core/RwaShiftDistributionRegistry.sol";

/// @dev `owner()`/`isAgent()`/`addAgent()` are implemented by T-REX's Token/IdentityRegistry
/// (via OwnableUpgradeable / AgentRoleUpgradeable) but are not part of the `IToken` /
/// `IIdentityRegistry` interfaces themselves. This minimal view lets tests assert on them without
/// depending on T-REX internals.
interface IOwnableAgentRole {
    function owner() external view returns (address);
    function isAgent(address agent) external view returns (bool);
    function addAgent(address agent) external;
}

/// @notice Shared fixture: deploys the full platform once per test and exposes a helper to spin up
/// a demo offering with configurable agents, mirroring what `SeedDemo.s.sol` does on Anvil.
abstract contract RwaShiftTestBase is Test {
    PlatformDeploy.Deployment internal deployment;

    address internal platformAdmin = makeAddr("platformAdmin");
    address internal issuerAdmin = makeAddr("issuerAdmin");
    address internal tokenAgent = makeAddr("tokenAgent");
    address internal identityBackend = makeAddr("identityBackend");
    address internal investorA = makeAddr("investorA");
    address internal investorB = makeAddr("investorB");
    address internal stranger = makeAddr("stranger");

    RwaShiftTokenFactory internal tokenFactory;
    RwaShiftIdentityGateway internal identityGateway;
    RwaShiftDistributionRegistry internal distributionRegistry;

    function setUp() public virtual {
        vm.startPrank(platformAdmin);
        deployment = PlatformDeploy.run(platformAdmin);
        vm.stopPrank();

        tokenFactory = RwaShiftTokenFactory(deployment.rwaShiftTokenFactory);
        identityGateway = RwaShiftIdentityGateway(deployment.rwaShiftIdentityGateway);
        distributionRegistry = RwaShiftDistributionRegistry(deployment.rwaShiftDistributionRegistry);

        // The identity backend is the Spring Boot service account: an IDENTITY_AGENT distinct
        // from PLATFORM_ADMIN, per the least-privilege requirement.
        vm.prank(platformAdmin);
        identityGateway.addAgent(identityBackend);
    }

    /// @dev Deploys a demo offering suite ("OFFERING-TEST") with a supply cap and returns the
    /// deployed token, wired identity registry, and identity/token agents as configured.
    function _createDemoOffering(uint256 supplyLimit) internal returns (IToken token, IIdentityRegistry ir) {
        return _createOffering("OFFERING-TEST", supplyLimit);
    }

    function _createOffering(string memory offeringRefId, uint256 supplyLimit)
        internal
        returns (IToken token, IIdentityRegistry ir)
    {
        address[] memory identityAgents = new address[](1);
        identityAgents[0] = address(identityGateway);
        address[] memory tokenAgents = new address[](1);
        tokenAgents[0] = tokenAgent;
        address[] memory complianceModules = new address[](1);
        complianceModules[0] = deployment.supplyLimitModule;
        bytes[] memory complianceSettings = new bytes[](1);
        complianceSettings[0] = abi.encodeWithSelector(SupplyLimitModule.setSupplyLimit.selector, supplyLimit);

        vm.prank(platformAdmin);
        address tokenAddr = tokenFactory.createOffering(
            offeringRefId,
            "Demo SPV Units",
            "DEMO",
            0,
            issuerAdmin,
            identityAgents,
            tokenAgents,
            complianceModules,
            complianceSettings
        );

        token = IToken(tokenAddr);
        ir = token.identityRegistry();

        vm.prank(tokenAgent);
        token.unpause();
    }

    function _registerInvestor(IIdentityRegistry ir, address investor, uint16 country) internal {
        vm.prank(identityBackend);
        identityGateway.registerInvestorIdentity(
            address(ir), investor, country, string(abi.encodePacked("salt-", investor))
        );
    }
}
