// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";
import { IIdentity } from "@onchain-id/solidity/contracts/interface/IIdentity.sol";
import { IModularCompliance } from "@trex/compliance/modular/IModularCompliance.sol";

import { RwaShiftTestBase, IOwnableAgentRole } from "../utils/RwaShiftTestBase.sol";

/// @notice End-to-end coverage of the offering lifecycle described in the product spec: issuance,
/// eligibility-gated transfers, freeze/unfreeze, recovery, and authorization boundaries around
/// each privileged action.
contract OfferingLifecycleTest is RwaShiftTestBase {
    IToken internal token;
    IIdentityRegistry internal ir;

    function setUp() public override {
        super.setUp();
        (token, ir) = _createDemoOffering(20_000);
        _registerInvestor(ir, investorA, 784);
    }

    // ---- issuance ----

    function test_mint_byTokenAgent_succeeds() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        assertEq(token.balanceOf(investorA), 100);
    }

    function test_mint_byNonAgent_reverts() public {
        vm.prank(stranger);
        vm.expectRevert();
        token.mint(investorA, 100);
    }

    function test_mint_toIneligibleWallet_reverts() public {
        vm.prank(tokenAgent);
        vm.expectRevert();
        token.mint(investorB, 100); // investorB never registered in this test
    }

    function test_mint_beyondSupplyLimit_reverts() public {
        vm.prank(tokenAgent);
        vm.expectRevert();
        token.mint(investorA, 20_001);
    }

    function test_mint_upToSupplyLimit_succeeds() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 20_000);
        assertEq(token.totalSupply(), 20_000);
    }

    // ---- eligibility-gated transfers ----

    function test_transfer_toUnverifiedWallet_reverts() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);

        vm.prank(investorA);
        vm.expectRevert();
        token.transfer(investorB, 10);
    }

    function test_transfer_toVerifiedWallet_succeeds() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        _registerInvestor(ir, investorB, 784);

        vm.prank(investorA);
        token.transfer(investorB, 10);

        assertEq(token.balanceOf(investorA), 90);
        assertEq(token.balanceOf(investorB), 10);
    }

    function test_transfer_afterRevocation_reverts() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        _registerInvestor(ir, investorB, 784);

        vm.prank(investorA);
        token.transfer(investorB, 10);

        vm.prank(identityBackend);
        identityGateway.revokeInvestorIdentity(address(ir), investorB);

        vm.prank(investorA);
        vm.expectRevert();
        token.transfer(investorB, 10);
    }

    // ---- freeze ----

    function test_freeze_blocksTransfersFromFrozenWallet() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        _registerInvestor(ir, investorB, 784);

        vm.prank(tokenAgent);
        token.setAddressFrozen(investorA, true);

        vm.prank(investorA);
        vm.expectRevert();
        token.transfer(investorB, 10);
    }

    function test_unfreeze_restoresTransferAbility() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        _registerInvestor(ir, investorB, 784);

        vm.startPrank(tokenAgent);
        token.setAddressFrozen(investorA, true);
        token.setAddressFrozen(investorA, false);
        vm.stopPrank();

        vm.prank(investorA);
        token.transfer(investorB, 10);
        assertEq(token.balanceOf(investorB), 10);
    }

    function test_freezePartialTokens_blocksOnlyFrozenPortion() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        _registerInvestor(ir, investorB, 784);

        vm.prank(tokenAgent);
        token.freezePartialTokens(investorA, 90);

        vm.prank(investorA);
        vm.expectRevert();
        token.transfer(investorB, 20); // only 10 free tokens available

        vm.prank(investorA);
        token.transfer(investorB, 10); // exactly the free balance succeeds
        assertEq(token.balanceOf(investorB), 10);
    }

    function test_freeze_byNonAgent_reverts() public {
        vm.prank(stranger);
        vm.expectRevert();
        token.setAddressFrozen(investorA, true);
    }

    // ---- burn ----

    function test_burn_byTokenAgent_succeeds() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);

        vm.prank(tokenAgent);
        token.burn(investorA, 40);
        assertEq(token.balanceOf(investorA), 60);
        assertEq(token.totalSupply(), 60);
    }

    function test_burn_byNonAgent_reverts() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);

        vm.prank(stranger);
        vm.expectRevert();
        token.burn(investorA, 40);
    }

    // ---- recovery ----

    function test_recoveryAddress_movesBalanceToNewWallet() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);

        address lostWalletIdentity = identityGateway.identityOf(investorA);
        address newWallet = makeAddr("investorA-newWallet");

        // Investor proves control of the new wallet by adding it as a management key on their
        // existing OnchainID (this is an investor-side action, independent of RWA Shift agents).
        vm.prank(investorA);
        IIdentity(lostWalletIdentity).addKey(keccak256(abi.encode(newWallet)), 1, 1);

        vm.prank(tokenAgent);
        bool ok = token.recoveryAddress(investorA, newWallet, lostWalletIdentity);

        assertTrue(ok);
        assertEq(token.balanceOf(investorA), 0);
        assertEq(token.balanceOf(newWallet), 100);
        assertTrue(ir.isVerified(newWallet));
        assertFalse(ir.contains(investorA));
    }

    function test_recoveryAddress_byNonAgent_reverts() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        address lostWalletIdentity = identityGateway.identityOf(investorA);
        address newWallet = makeAddr("investorA-newWallet");

        vm.prank(investorA);
        IIdentity(lostWalletIdentity).addKey(keccak256(abi.encode(newWallet)), 1, 1);

        vm.prank(stranger);
        vm.expectRevert();
        token.recoveryAddress(investorA, newWallet, lostWalletIdentity);
    }

    function test_recoveryAddress_withoutManagementKey_reverts() public {
        vm.prank(tokenAgent);
        token.mint(investorA, 100);
        address lostWalletIdentity = identityGateway.identityOf(investorA);
        address newWallet = makeAddr("investorA-newWallet-noKey");

        vm.prank(tokenAgent);
        vm.expectRevert("Recovery not possible");
        token.recoveryAddress(investorA, newWallet, lostWalletIdentity);
    }

    // ---- compliance/identity administration boundaries ----

    function test_addComplianceModule_byNonOwner_reverts() public {
        IModularCompliance mc = token.compliance();
        vm.prank(stranger);
        vm.expectRevert("Ownable: caller is not the owner");
        mc.addModule(deployment.supplyLimitModule);
    }

    function test_addTokenAgent_byNonOwner_reverts() public {
        vm.prank(stranger);
        vm.expectRevert("Ownable: caller is not the owner");
        IOwnableAgentRole(address(token)).addAgent(stranger);
    }

    function test_issuerAdmin_canManageTokenAgents() public {
        address newAgent = makeAddr("secondTokenAgent");
        vm.prank(issuerAdmin);
        IOwnableAgentRole(address(token)).addAgent(newAgent);
        assertTrue(IOwnableAgentRole(address(token)).isAgent(newAgent));
    }
}
