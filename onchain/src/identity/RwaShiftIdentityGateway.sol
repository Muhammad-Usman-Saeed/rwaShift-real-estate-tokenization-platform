// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { AgentRole } from "@trex/roles/AgentRole.sol";
import { IIdFactory } from "@onchain-id/solidity/contracts/factory/IIdFactory.sol";
import { IIdentity } from "@onchain-id/solidity/contracts/interface/IIdentity.sol";
import { IIdentityRegistry } from "@trex/registry/interface/IIdentityRegistry.sol";

/// @title RwaShiftIdentityGateway
/// @notice The only path by which the off-chain KYC/eligibility decision (made in Spring Boot)
/// is translated into an on-chain, ERC-3643-recognized investor identity.
///
/// Responsibility
/// --------------
/// V1 does not implement KYC in Solidity. Spring Boot performs (simulated) KYC, decides an
/// investor is eligible, and then calls this contract to (a) get-or-create the investor's
/// OnchainID identity via the platform's `IIdFactory`, and (b) register/update/revoke that
/// identity in the relevant offering's `IdentityRegistry`. No documents, PII, or claim payloads
/// are ever passed in — only wallet addresses, an opaque per-wallet salt, and an ISO-3166
/// numeric country code (already the minimum data ERC-3643 itself requires on-chain).
///
/// Trust assumptions
/// ------------------
/// - This contract must own the platform's `IIdFactory` (OnchainID identities are created through
///   it) and must be added as an `IdentityRegistry` agent on every offering suite that wants to
///   use it (done automatically by `RwaShiftTokenFactory.createOffering` via `identityAgents`).
/// - `IDENTITY_AGENT` (the `onlyAgent` role from T-REX's `AgentRole`) is granted only to backend
///   service accounts (Spring Boot's signing wallet, or an operations multisig) — never to
///   investors or issuers directly. This is the on-chain enforcement point for "unauthorized
///   accounts cannot modify identity".
///
/// Upgradeability decision: IMMUTABLE.
/// Pure pass-through/coordination logic over two already-audited, independently upgradeable
/// systems (OnchainID's `IdFactory` and T-REX's `IdentityRegistry`). No storage worth migrating.
///
/// Security boundary
/// ------------------
/// Every state-changing function is `onlyAgent`. Read functions are unrestricted. This contract
/// never stores KYC documents or PII — it only ever forwards a wallet address, an OnchainID
/// address it deployed, and a country code.
contract RwaShiftIdentityGateway is AgentRole {
    IIdFactory public immutable idFactory;

    error ZeroAddress();

    event InvestorIdentityRegistered(
        address indexed identityRegistry, address indexed wallet, address indexed identity, uint16 country
    );
    event InvestorIdentityUpdated(address indexed identityRegistry, address indexed wallet, uint16 country);
    event InvestorIdentityRevoked(address indexed identityRegistry, address indexed wallet);

    constructor(address idFactory_) {
        if (idFactory_ == address(0)) revert ZeroAddress();
        idFactory = IIdFactory(idFactory_);
    }

    /// @notice Registers an investor as eligible to hold units of one offering token.
    /// @dev Reuses an existing OnchainID identity for the wallet if one was already deployed
    /// (e.g. the investor already holds units in another offering); otherwise deploys a new one
    /// via the platform `IIdFactory` using CREATE2 with `salt`.
    /// @param identityRegistry The offering's `IdentityRegistry` (from `IToken.identityRegistry()`).
    /// @param wallet The investor's wallet address.
    /// @param country ISO-3166-1 numeric country code, as required by ERC-3643.
    /// @param salt Unique, deterministic salt for the OnchainID CREATE2 deployment (only consumed
    /// if the wallet has no identity yet).
    function registerInvestorIdentity(address identityRegistry, address wallet, uint16 country, string calldata salt)
        external
        onlyAgent
        returns (address identity)
    {
        if (identityRegistry == address(0) || wallet == address(0)) revert ZeroAddress();

        identity = idFactory.getIdentity(wallet);
        if (identity == address(0)) {
            identity = idFactory.createIdentity(wallet, salt);
        }

        IIdentityRegistry(identityRegistry).registerIdentity(wallet, IIdentity(identity), country);
        emit InvestorIdentityRegistered(identityRegistry, wallet, identity, country);
    }

    /// @notice Updates an already-registered investor's country (e.g. after a re-KYC).
    function updateInvestorCountry(address identityRegistry, address wallet, uint16 country) external onlyAgent {
        if (identityRegistry == address(0) || wallet == address(0)) revert ZeroAddress();
        IIdentityRegistry(identityRegistry).updateCountry(wallet, country);
        emit InvestorIdentityUpdated(identityRegistry, wallet, country);
    }

    /// @notice Revokes an investor's eligibility for one offering (e.g. sanctions hit, withdrawal
    /// of consent). Does not burn or transfer the investor's existing balance — that is a
    /// separate, explicitly authorized token-agent action (freeze/recover) if ever required.
    function revokeInvestorIdentity(address identityRegistry, address wallet) external onlyAgent {
        if (identityRegistry == address(0) || wallet == address(0)) revert ZeroAddress();
        IIdentityRegistry(identityRegistry).deleteIdentity(wallet);
        emit InvestorIdentityRevoked(identityRegistry, wallet);
    }

    /// @notice Returns the investor's OnchainID identity address, or the zero address if none
    /// has been deployed yet.
    function identityOf(address wallet) external view returns (address) {
        return idFactory.getIdentity(wallet);
    }
}
