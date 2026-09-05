// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";

import { RwaShiftTestBase } from "../utils/RwaShiftTestBase.sol";
import { RwaShiftIdentityGateway } from "../../src/identity/RwaShiftIdentityGateway.sol";

contract RwaShiftIdentityGatewayTest is RwaShiftTestBase {
    IIdentityRegistry internal ir;

    function setUp() public override {
        super.setUp();
        (, ir) = _createDemoOffering(1_000);
    }

    function test_registerInvestorIdentity_marksWalletVerified() public {
        assertFalse(ir.isVerified(investorA));
        _registerInvestor(ir, investorA, 784);
        assertTrue(ir.isVerified(investorA));
        assertTrue(identityGateway.identityOf(investorA) != address(0));
    }

    function test_registerInvestorIdentity_revertsForNonAgent() public {
        vm.prank(stranger);
        vm.expectRevert("AgentRole: caller does not have the Agent role");
        identityGateway.registerInvestorIdentity(address(ir), investorA, 784, "salt");
    }

    function test_registerInvestorIdentity_reusesExistingOnchainId() public {
        _registerInvestor(ir, investorA, 784);
        address identity1 = identityGateway.identityOf(investorA);

        // Second offering, same investor: identity should be reused, not redeployed.
        (, IIdentityRegistry ir2) = _createOffering("OFFERING-SECOND", 1_000);
        vm.prank(identityBackend);
        identityGateway.registerInvestorIdentity(address(ir2), investorA, 784, "different-salt-ignored");

        assertEq(identityGateway.identityOf(investorA), identity1, "identity should be reused across offerings");
        assertTrue(ir2.isVerified(investorA));
    }

    function test_updateInvestorCountry_revertsForNonAgent() public {
        _registerInvestor(ir, investorA, 784);
        vm.prank(stranger);
        vm.expectRevert("AgentRole: caller does not have the Agent role");
        identityGateway.updateInvestorCountry(address(ir), investorA, 250);
    }

    function test_updateInvestorCountry_succeedsForAgent() public {
        _registerInvestor(ir, investorA, 784);
        vm.prank(identityBackend);
        identityGateway.updateInvestorCountry(address(ir), investorA, 250);
        assertEq(ir.investorCountry(investorA), 250);
    }

    function test_revokeInvestorIdentity_removesEligibility() public {
        _registerInvestor(ir, investorA, 784);
        assertTrue(ir.isVerified(investorA));

        vm.prank(identityBackend);
        identityGateway.revokeInvestorIdentity(address(ir), investorA);
        assertFalse(ir.isVerified(investorA));
    }

    function test_revokeInvestorIdentity_revertsForNonAgent() public {
        _registerInvestor(ir, investorA, 784);
        vm.prank(stranger);
        vm.expectRevert("AgentRole: caller does not have the Agent role");
        identityGateway.revokeInvestorIdentity(address(ir), investorA);
    }

    function test_registerInvestorIdentity_revertsOnZeroWallet() public {
        vm.prank(identityBackend);
        vm.expectRevert(RwaShiftIdentityGateway.ZeroAddress.selector);
        identityGateway.registerInvestorIdentity(address(ir), address(0), 784, "salt");
    }

    function test_onlyOwnerCanAddAgent() public {
        vm.prank(stranger);
        vm.expectRevert("Ownable: caller is not the owner");
        identityGateway.addAgent(stranger);
    }
}
