// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { Test } from "forge-std/Test.sol";

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";

/// @notice Bounded, randomized actor for the token-accounting invariant suite. Every action is
/// clamped to succeed-or-safely-no-op so the invariant run explores real state transitions instead
/// of mostly reverting.
contract TokenAccountingHandler is Test {
    uint256 public constant SUPPLY_LIMIT = 20_000;

    IToken public immutable token;
    IIdentityRegistry public immutable ir;
    address public immutable tokenAgent;

    address[] public eligibleInvestors;
    address public immutable ineligibleInvestor;

    constructor(
        IToken token_,
        IIdentityRegistry ir_,
        address tokenAgent_,
        address[] memory eligibleInvestors_,
        address ineligibleInvestor_
    ) {
        token = token_;
        ir = ir_;
        tokenAgent = tokenAgent_;
        eligibleInvestors = eligibleInvestors_;
        ineligibleInvestor = ineligibleInvestor_;
    }

    function mint(uint256 investorSeed, uint256 amount) external {
        address to = _pickEligible(investorSeed);
        uint256 room = SUPPLY_LIMIT - token.totalSupply();
        amount = bound(amount, 0, room);
        if (amount == 0) return;

        vm.prank(tokenAgent);
        token.mint(to, amount);
    }

    function transfer(uint256 fromSeed, uint256 toSeed, uint256 amount) external {
        address from = _pickEligible(fromSeed);
        address to = _pickEligible(toSeed);
        // A transfer requires neither side to be frozen (Token.transfer checks both `_to` and
        // `msg.sender`), so the handler must skip whenever either party is frozen.
        if (token.isFrozen(from) || token.isFrozen(to)) return;
        uint256 freeBalance = token.balanceOf(from) - token.getFrozenTokens(from);
        if (freeBalance == 0) return;
        amount = bound(amount, 0, freeBalance);
        if (amount == 0) return;

        vm.prank(from);
        token.transfer(to, amount);
    }

    function freeze(uint256 investorSeed, bool frozen) external {
        address who = _pickEligible(investorSeed);
        vm.prank(tokenAgent);
        token.setAddressFrozen(who, frozen);
    }

    function burn(uint256 investorSeed, uint256 amount) external {
        address who = _pickEligible(investorSeed);
        uint256 freeBalance = token.balanceOf(who) - token.getFrozenTokens(who);
        amount = bound(amount, 0, freeBalance);
        if (amount == 0) return;

        vm.prank(tokenAgent);
        token.burn(who, amount);
    }

    /// @dev Attempts a mint to the never-registered investor; must always fail and must never
    /// change total supply. Modeled as a no-op-on-success handler action so the fuzzer keeps
    /// exploring rather than the whole run failing outright.
    function attemptMintToIneligible(uint256 amount) external {
        amount = bound(amount, 1, 1_000);
        vm.prank(tokenAgent);
        try token.mint(ineligibleInvestor, amount) {
            assert(false); // should never succeed
        } catch {
            // expected
        }
    }

    function eligibleInvestorsCount() external view returns (uint256) {
        return eligibleInvestors.length;
    }

    function _pickEligible(uint256 seed) internal view returns (address) {
        return eligibleInvestors[seed % eligibleInvestors.length];
    }
}
