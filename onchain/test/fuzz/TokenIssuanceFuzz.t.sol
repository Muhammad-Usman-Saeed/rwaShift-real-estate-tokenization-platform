// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";

import { RwaShiftTestBase } from "../utils/RwaShiftTestBase.sol";

contract TokenIssuanceFuzzTest is RwaShiftTestBase {
    uint256 internal constant SUPPLY_LIMIT = 20_000;

    IToken internal token;
    IIdentityRegistry internal ir;

    function setUp() public override {
        super.setUp();
        (token, ir) = _createDemoOffering(SUPPLY_LIMIT);
        _registerInvestor(ir, investorA, 784);
    }

    /// @dev Issuance must never exceed the offering's configured on-chain supply cap, for any
    /// amount an agent attempts to mint.
    function testFuzz_mint_neverExceedsSupplyLimit(uint256 amount) public {
        // T-REX's ModularCompliance rejects zero-value mints outright ("invalid argument - no
        // value mint"), independent of the supply cap, so zero is excluded here.
        amount = bound(amount, 1, SUPPLY_LIMIT * 2);

        vm.prank(tokenAgent);
        if (amount > SUPPLY_LIMIT) {
            vm.expectRevert();
            token.mint(investorA, amount);
            assertEq(token.totalSupply(), 0);
        } else {
            token.mint(investorA, amount);
            assertEq(token.totalSupply(), amount);
            assertLe(token.totalSupply(), SUPPLY_LIMIT);
        }
    }

    /// @dev Sequential mints never push total supply past the cap, regardless of split.
    function testFuzz_mint_sequentialNeverExceedsCap(uint256 first, uint256 second) public {
        first = bound(first, 1, SUPPLY_LIMIT);
        second = bound(second, 1, SUPPLY_LIMIT);

        vm.startPrank(tokenAgent);
        token.mint(investorA, first);

        if (first + second > SUPPLY_LIMIT) {
            vm.expectRevert();
            token.mint(investorA, second);
        } else {
            token.mint(investorA, second);
        }
        vm.stopPrank();

        assertLe(token.totalSupply(), SUPPLY_LIMIT);
    }

    /// @dev A transfer's destination eligibility is the sole gate: any registered wallet can
    /// receive, any unregistered wallet cannot, regardless of transfer amount.
    function testFuzz_transfer_gatedPurelyByRecipientEligibility(uint256 amount, bool recipientEligible) public {
        vm.prank(tokenAgent);
        token.mint(investorA, 1_000);
        // Zero-value transfers are rejected by ModularCompliance regardless of eligibility, so
        // this fuzz test only targets the eligibility gate itself.
        amount = bound(amount, 1, 1_000);

        address recipient = makeAddr("fuzzRecipient");
        if (recipientEligible) {
            _registerInvestor(ir, recipient, 784);
        }

        vm.prank(investorA);
        if (recipientEligible) {
            token.transfer(recipient, amount);
            assertEq(token.balanceOf(recipient), amount);
        } else {
            vm.expectRevert();
            token.transfer(recipient, amount);
        }
    }

    /// @dev Frozen wallets can never move tokens out, for any transfer amount.
    function testFuzz_frozenWallet_cannotTransfer(uint256 amount) public {
        vm.prank(tokenAgent);
        token.mint(investorA, 1_000);
        amount = bound(amount, 1, 1_000);

        _registerInvestor(ir, investorB, 784);

        vm.prank(tokenAgent);
        token.setAddressFrozen(investorA, true);

        vm.prank(investorA);
        vm.expectRevert();
        token.transfer(investorB, amount);
    }
}
