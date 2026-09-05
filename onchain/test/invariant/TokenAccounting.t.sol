// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { IToken } from "@trex/token/IToken.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";

import { RwaShiftTestBase } from "../utils/RwaShiftTestBase.sol";
import { TokenAccountingHandler } from "./TokenAccountingHandler.sol";

/// @notice Invariants that must hold no matter what sequence of (bounded, always-authorized-actor)
/// mint/transfer/freeze/burn actions occurs: accounting stays internally consistent, issuance never
/// exceeds the offering's on-chain-enforced cap, and ineligible wallets never end up holding units.
contract TokenAccountingInvariantTest is RwaShiftTestBase {
    IToken internal token;
    IIdentityRegistry internal ir;
    TokenAccountingHandler internal handler;

    address[] internal eligibleInvestors;

    function setUp() public override {
        super.setUp();
        (token, ir) = _createDemoOffering(20_000);

        for (uint256 i = 0; i < 5; i++) {
            address investor = makeAddr(string(abi.encodePacked("invariantInvestor", i)));
            _registerInvestor(ir, investor, 784);
            eligibleInvestors.push(investor);
        }

        handler = new TokenAccountingHandler(token, ir, tokenAgent, eligibleInvestors, investorB);

        targetContract(address(handler));
    }

    function invariant_totalSupplyNeverExceedsCap() public view {
        assertLe(token.totalSupply(), handler.SUPPLY_LIMIT());
    }

    function invariant_sumOfBalancesEqualsTotalSupply() public view {
        uint256 sum;
        uint256 n = handler.eligibleInvestorsCount();
        for (uint256 i = 0; i < n; i++) {
            sum += token.balanceOf(handler.eligibleInvestors(i));
        }
        assertEq(sum, token.totalSupply());
    }

    function invariant_ineligibleInvestorNeverHoldsUnits() public view {
        assertEq(token.balanceOf(investorB), 0);
        assertFalse(ir.isVerified(investorB));
    }

    function invariant_frozenAmountNeverExceedsBalance() public view {
        uint256 n = handler.eligibleInvestorsCount();
        for (uint256 i = 0; i < n; i++) {
            address investor = handler.eligibleInvestors(i);
            assertLe(token.getFrozenTokens(investor), token.balanceOf(investor));
        }
    }
}
