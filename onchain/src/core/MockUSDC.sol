// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { ERC20 } from "@openzeppelin/contracts/token/ERC20/ERC20.sol";

/// @title MockUSDC
/// @notice A 6-decimal ERC20 standing in for real USDC on local/testnet deployments, so the
/// crypto-payment flow (investor wallet -> platform's payment-collection address) can be
/// exercised end to end without depending on a real stablecoin's testnet faucet/liquidity.
///
/// Deployment scope: LOCAL/TESTNET ONLY. `DeployLocal.s.sol` deploys this and mints an initial
/// balance to each demo investor; `DeploySepolia.s.sol` should point `USDC_TOKEN_ADDRESS` at a
/// real Sepolia USDC contract instead of deploying this one — see onchain/README.md. This
/// contract must never be deployed to mainnet; `mint` is intentionally unrestricted (anyone can
/// mint to anyone) since the token carries no real value in the environments it's meant for.
contract MockUSDC is ERC20 {
    constructor() ERC20("Mock USD Coin", "USDC") {}

    function decimals() public pure override returns (uint8) {
        return 6;
    }

    /// @notice Unrestricted on purpose — see contract-level NatSpec. Lets the demo seed script,
    /// or a developer testing the flow manually, fund any wallet without needing ETH-for-gas
    /// bootstrapping tricks or a faucet.
    function mint(address to, uint256 amount) external {
        _mint(to, amount);
    }
}
