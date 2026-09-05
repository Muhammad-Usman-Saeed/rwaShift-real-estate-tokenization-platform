// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";
import { IModularCompliance } from "@trex/compliance/modular/IModularCompliance.sol";

import { RwaShiftTestBase, IOwnableAgentRole } from "../utils/RwaShiftTestBase.sol";
import { RwaShiftTokenFactory } from "../../src/core/RwaShiftTokenFactory.sol";

contract RwaShiftTokenFactoryTest is RwaShiftTestBase {
    // Mirrors RwaShiftTokenFactory.OfferingTokenCreated for vm.expectEmit (Solidity 0.8.17 does
    // not support emitting another contract's qualified event name).
    event OfferingTokenCreated(
        string offeringRefId,
        address indexed token,
        address indexed issuerAdmin,
        string name,
        string symbol,
        uint8 decimals,
        address identityRegistry,
        address compliance
    );

    function test_createOffering_deploysWiredSuite() public {
        (IToken token, IIdentityRegistry ir) = _createDemoOffering(1_000);

        assertEq(IOwnableAgentRole(address(token)).owner(), issuerAdmin, "token owner should be the issuer admin");
        assertTrue(IOwnableAgentRole(address(token)).isAgent(tokenAgent), "token agent should be set");
        assertTrue(
            IOwnableAgentRole(address(ir)).isAgent(address(identityGateway)), "identity gateway should be an IR agent"
        );
        assertEq(tokenFactory.tokenForOffering("OFFERING-TEST"), address(token));
        assertEq(tokenFactory.offeringForToken(address(token)), "OFFERING-TEST");

        IModularCompliance mc = token.compliance();
        assertTrue(mc.isModuleBound(deployment.supplyLimitModule), "supply limit module should be bound");
    }

    function test_createOffering_emitsEvent() public {
        address[] memory identityAgents = new address[](1);
        identityAgents[0] = address(identityGateway);
        address[] memory tokenAgents = new address[](1);
        tokenAgents[0] = tokenAgent;

        vm.prank(platformAdmin);
        vm.expectEmit(false, true, false, false, address(tokenFactory));
        emit OfferingTokenCreated(
            "OFFERING-EVT", address(0), issuerAdmin, "Demo SPV Units", "DEMO", 0, address(0), address(0)
        );
        tokenFactory.createOffering(
            "OFFERING-EVT",
            "Demo SPV Units",
            "DEMO",
            0,
            issuerAdmin,
            identityAgents,
            tokenAgents,
            new address[](0),
            new bytes[](0)
        );
    }

    function test_createOffering_revertsForNonIssuerAdmin() public {
        vm.prank(stranger);
        vm.expectRevert();
        tokenFactory.createOffering(
            "OFFERING-DENY",
            "Demo",
            "DEMO",
            0,
            issuerAdmin,
            new address[](0),
            new address[](0),
            new address[](0),
            new bytes[](0)
        );
    }

    function test_createOffering_revertsOnDuplicateOfferingRef() public {
        _createDemoOffering(1_000);

        vm.prank(platformAdmin);
        vm.expectRevert(abi.encodeWithSelector(RwaShiftTokenFactory.OfferingAlreadyExists.selector, "OFFERING-TEST"));
        tokenFactory.createOffering(
            "OFFERING-TEST",
            "Demo SPV Units",
            "DEMO",
            0,
            issuerAdmin,
            new address[](0),
            new address[](0),
            new address[](0),
            new bytes[](0)
        );
    }

    function test_createOffering_revertsOnZeroIssuerAdmin() public {
        vm.prank(platformAdmin);
        vm.expectRevert(RwaShiftTokenFactory.ZeroAddress.selector);
        tokenFactory.createOffering(
            "OFFERING-ZERO",
            "Demo",
            "DEMO",
            0,
            address(0),
            new address[](0),
            new address[](0),
            new address[](0),
            new bytes[](0)
        );
    }

    function test_createOffering_revertsOnEmptyOfferingRef() public {
        vm.prank(platformAdmin);
        vm.expectRevert(RwaShiftTokenFactory.EmptyOfferingRef.selector);
        tokenFactory.createOffering(
            "", "Demo", "DEMO", 0, issuerAdmin, new address[](0), new address[](0), new address[](0), new bytes[](0)
        );
    }

    function test_grantIssuerAdmin_allowsNewIssuerToCreateOfferings() public {
        address newIssuerOps = makeAddr("newIssuerOps");
        bytes32 issuerAdminRole = tokenFactory.ISSUER_ADMIN_ROLE();

        vm.prank(platformAdmin);
        tokenFactory.grantRole(issuerAdminRole, newIssuerOps);

        vm.prank(newIssuerOps);
        address token = tokenFactory.createOffering(
            "OFFERING-NEW-ISSUER",
            "Demo",
            "DEMO",
            0,
            issuerAdmin,
            new address[](0),
            new address[](0),
            new address[](0),
            new bytes[](0)
        );
        assertTrue(token != address(0));
    }

    function test_offeringForToken_revertsForUnknownToken() public {
        vm.expectRevert();
        tokenFactory.offeringForToken(address(0xdead));
    }
}
