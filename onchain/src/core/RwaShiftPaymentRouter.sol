// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { IERC20 } from "@openzeppelin/contracts/token/ERC20/IERC20.sol";

/// @title RwaShiftPaymentRouter
/// @notice The one and only crypto-payment entry point investors use — routes a USDC payment to
/// the platform's collection address while tagging it, unambiguously and on-chain, with which
/// investment it pays for.
///
/// Why this exists (see docs/critical-analysis.md #3): a plain ERC20 `transfer` carries no
/// reference field, so matching an incoming payment to a pending investment previously had to
/// guess by `(payer wallet, exact amount)` — which breaks the moment one investor has two pending
/// investments of the same amount at once. Routing every payment through `payInvestment` instead
/// means the backend's `CryptoPaymentWatcher` reads the investment id straight out of the
/// `PaymentReceived` event, with zero ambiguity, regardless of how many pending investments any
/// investor has or what they cost.
///
/// This contract never custodies funds — `payInvestment` immediately forwards the full amount to
/// `collectionAddress` via `transferFrom` in the same transaction (the investor `approve`s this
/// contract first, standard two-step ERC20 payment UX). If forwarding fails, the whole payment
/// reverts; there's no partial/stuck state to recover.
///
/// Upgradeability decision: IMMUTABLE. This is a thin, single-purpose router with no state worth
/// migrating — if the payment token or collection address ever needs to change, deploy a new
/// router and point `PAYMENT_ROUTER_ADDRESS` at it, same as any other reconfiguration.
contract RwaShiftPaymentRouter {
    IERC20 public immutable usdc;
    address public immutable collectionAddress;

    error ZeroAddress();
    error ZeroAmount();
    error EmptyInvestmentId();

    /// @param investmentId Non-indexed so the raw string is recoverable from log data, not just
    /// its hash — an indexed dynamic type only stores a hash in the topic, which the backend
    /// couldn't reverse back into an investment id to look up.
    event PaymentReceived(address indexed payer, string investmentId, uint256 amount);

    constructor(address usdcToken, address paymentCollectionAddress) {
        if (usdcToken == address(0) || paymentCollectionAddress == address(0)) revert ZeroAddress();
        usdc = IERC20(usdcToken);
        collectionAddress = paymentCollectionAddress;
    }

    /// @param investmentId The off-chain investment id (ULID) this payment settles. Opaque to
    /// this contract — carried through purely as a tag for the backend to match on.
    /// @param amount USDC amount (6 decimals), must exactly match what the investor `approve`d.
    function payInvestment(string calldata investmentId, uint256 amount) external {
        if (amount == 0) revert ZeroAmount();
        if (bytes(investmentId).length == 0) revert EmptyInvestmentId();

        bool ok = usdc.transferFrom(msg.sender, collectionAddress, amount);
        require(ok, "USDC transfer failed");

        emit PaymentReceived(msg.sender, investmentId, amount);
    }
}
