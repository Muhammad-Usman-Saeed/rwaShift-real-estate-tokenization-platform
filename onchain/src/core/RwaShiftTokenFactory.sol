// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { AccessControl } from "@openzeppelin/contracts/access/AccessControl.sol";
import { ITREXFactory } from "@trex/factory/ITREXFactory.sol";
import { IToken } from "@trex/token/IToken.sol";

/// @title RwaShiftTokenFactory
/// @notice Deploys and tracks per-offering ERC-3643 (T-REX) token suites on behalf of RWA Shift issuers.
///
/// Responsibility
/// --------------
/// This contract is the single entry point the platform uses to turn an *approved* investment
/// offering (an off-chain, legally-structured investment interest in an SPV that itself holds a
/// real-estate asset) into on-chain ERC-3643 investment units. It does not model the property,
/// the SPV, the offering economics or any legal document — those remain in Spring Boot/MySQL.
/// It composes the audited T-REX `TREXFactory` (CREATE2 suite deployer) rather than re-implementing
/// token/registry/compliance deployment logic, per the "do not rewrite ERC-3643" mandate.
///
/// Trust assumptions
/// ------------------
/// - This contract must be the `owner` of the underlying `TREXFactory` (that ownership transfer is
///   performed once, by the deployment script, right after both contracts are deployed).
/// - `ISSUER_ADMIN_ROLE` holders are platform-approved back-office operators (e.g. the Spring Boot
///   service account acting on behalf of an approved issuer) who may request new offering token
///   suites. They cannot touch already-deployed suites — control over a suite is handed to the
///   `issuerAdmin` address supplied in `createOffering`, which becomes the T-REX suite owner.
/// - `DEFAULT_ADMIN_ROLE` (PLATFORM_ADMIN) may grant/revoke `ISSUER_ADMIN_ROLE` and perform
///   emergency ownership recovery on the underlying `TREXFactory`.
///
/// Upgradeability decision: IMMUTABLE.
/// This contract holds no user funds and has a narrow, stable responsibility (map an offering
/// reference ID to a freshly deployed T-REX suite). The parts of the system that legitimately need
/// upgradeability (the T-REX implementation contracts themselves) already get it through
/// `TREXImplementationAuthority`. Adding proxy machinery here would only grow the trusted base
/// without adding capability.
///
/// Security boundary
/// ------------------
/// `createOffering` is the only state-changing entry point exposed to issuer operators. It never
/// accepts property metadata, valuations, or PII — only opaque identifiers (`offeringRefId`) and
/// the addresses/parameters required by ERC-3643 (name, symbol, decimals, owner, agents, compliance
/// module wiring). Everything else is read-only.
contract RwaShiftTokenFactory is AccessControl {
    bytes32 public constant ISSUER_ADMIN_ROLE = keccak256("ISSUER_ADMIN_ROLE");

    /// @notice The audited T-REX factory this contract composes over. Immutable: the platform
    /// deploys exactly one TREXFactory (backed by one TREXImplementationAuthority) and this
    /// contract is granted ownership of it post-deployment.
    ITREXFactory public immutable trexFactory;

    /// @dev offeringRefId => deployed token address
    mapping(string => address) private _offeringToken;
    /// @dev token address => offeringRefId (reverse lookup for reconciliation)
    mapping(address => string) private _tokenOffering;

    error ZeroAddress();
    error EmptyOfferingRef();
    error OfferingAlreadyExists(string offeringRefId);
    error OfferingNotFound(string offeringRefId);

    /// @notice Emitted once an offering's full ERC-3643 token suite has been deployed.
    /// @param offeringRefId Opaque business reference ID owned by Spring Boot (never property
    /// metadata or PII); links the on-chain suite back to the off-chain offering record.
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

    constructor(address trexFactory_, address platformAdmin) {
        if (trexFactory_ == address(0) || platformAdmin == address(0)) revert ZeroAddress();
        trexFactory = ITREXFactory(trexFactory_);
        _grantRole(DEFAULT_ADMIN_ROLE, platformAdmin);
        _grantRole(ISSUER_ADMIN_ROLE, platformAdmin);
    }

    /// @notice Deploys a complete ERC-3643 token suite (Token, IdentityRegistry, IdentityRegistryStorage,
    /// ClaimTopicsRegistry, TrustedIssuersRegistry, ModularCompliance) for one approved offering.
    /// @dev `offeringRefId` is used as the CREATE2 salt, so it doubles as a global uniqueness key and
    /// yields a deterministic token address. Property/legal/offering-economics metadata is intentionally
    /// not accepted here — only what ERC-3643 itself requires.
    /// @param offeringRefId Opaque, off-chain-issued business reference ID for the offering.
    /// @param name Token name (e.g. "Dubai Business Tower SPV Units").
    /// @param symbol Token symbol.
    /// @param decimals Token decimals (0-18, matches ERC-3643 unit granularity for the offering).
    /// @param issuerAdmin Address that becomes owner of the deployed suite (ISSUER_ADMIN for this offering).
    /// @param identityAgents Addresses granted the IdentityRegistry agent role (e.g. the platform's
    /// identity gateway, so it can register/update/revoke investor eligibility).
    /// @param tokenAgents Addresses granted the Token agent role (mint/burn/freeze/pause/recovery).
    /// @param complianceModules T-REX modular-compliance modules to bind (e.g. `SupplyLimitModule` to
    /// cap on-chain issuance at the offering's configured unit count). Reused from the audited T-REX
    /// module library — no custom compliance logic is introduced here.
    /// @param complianceSettings ABI-encoded init calls corresponding 1:1 to `complianceModules`.
    function createOffering(
        string calldata offeringRefId,
        string calldata name,
        string calldata symbol,
        uint8 decimals,
        address issuerAdmin,
        address[] calldata identityAgents,
        address[] calldata tokenAgents,
        address[] calldata complianceModules,
        bytes[] calldata complianceSettings
    ) external onlyRole(ISSUER_ADMIN_ROLE) returns (address token) {
        if (bytes(offeringRefId).length == 0) revert EmptyOfferingRef();
        if (issuerAdmin == address(0)) revert ZeroAddress();
        if (_offeringToken[offeringRefId] != address(0)) revert OfferingAlreadyExists(offeringRefId);

        ITREXFactory.TokenDetails memory tokenDetails = ITREXFactory.TokenDetails({
            owner: issuerAdmin,
            name: name,
            symbol: symbol,
            decimals: decimals,
            irs: address(0),
            ONCHAINID: address(0),
            irAgents: identityAgents,
            tokenAgents: tokenAgents,
            complianceModules: complianceModules,
            complianceSettings: complianceSettings
        });

        // V1: KYC/eligibility is simulated off-chain and reduced on-chain to "identity is
        // registered in the IdentityRegistry" (see RwaShiftIdentityGateway). No claim topics are
        // required from trusted issuers yet, so an empty ClaimDetails is intentional, not a stub.
        ITREXFactory.ClaimDetails memory claimDetails = ITREXFactory.ClaimDetails({
            claimTopics: new uint256[](0), issuers: new address[](0), issuerClaims: new uint256[][](0)
        });

        trexFactory.deployTREXSuite(offeringRefId, tokenDetails, claimDetails);
        token = trexFactory.getToken(offeringRefId);

        _offeringToken[offeringRefId] = token;
        _tokenOffering[token] = offeringRefId;

        IToken deployed = IToken(token);
        emit OfferingTokenCreated(
            offeringRefId,
            token,
            issuerAdmin,
            name,
            symbol,
            decimals,
            address(deployed.identityRegistry()),
            address(deployed.compliance())
        );
    }

    /// @notice Recovers ownership of a contract mistakenly left owned by the underlying TREXFactory
    /// (e.g. an IdentityRegistryStorage shared across offerings). Platform-admin only, mirrors
    /// `ITREXFactory.recoverContractOwnership`.
    function recoverContractOwnership(address contractAddress, address newOwner) external onlyRole(DEFAULT_ADMIN_ROLE) {
        trexFactory.recoverContractOwnership(contractAddress, newOwner);
    }

    function tokenForOffering(string calldata offeringRefId) external view returns (address) {
        return _offeringToken[offeringRefId];
    }

    function offeringForToken(address token) external view returns (string memory offeringRefId) {
        offeringRefId = _tokenOffering[token];
        if (bytes(offeringRefId).length == 0) revert OfferingNotFound("");
    }
}
