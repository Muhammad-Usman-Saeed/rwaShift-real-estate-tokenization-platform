// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { AccessControl } from "@openzeppelin/contracts/access/AccessControl.sol";

/// @title RwaShiftDistributionRegistry
/// @notice Minimal, append-only on-chain audit trail for off-chain distribution (dividend/rental
/// income) events, recorded per offering token.
///
/// Responsibility
/// --------------
/// Distribution *calculation* and *settlement* are off-chain (Spring Boot computes each investor's
/// pro-rata share and pays out through whatever rail the offering uses). This contract's only job
/// is to give each distribution run an immutable, timestamped, on-chain anchor that Spring Boot's
/// records can be reconciled against and that investors can independently verify was not silently
/// altered after the fact. It intentionally stores no investor-level payment or bank data.
///
/// Trust assumptions
/// ------------------
/// `DISTRIBUTION_AGENT_ROLE` holders (the Spring Boot distribution service, or an operations
/// multisig) are the only accounts that can record a distribution. Records are immutable once
/// written — there is no update/delete function, since amending history would defeat the purpose
/// of an audit trail; a correction is recorded as a new distribution reference with a note
/// off-chain, exactly like a ledger correcting entry.
///
/// Upgradeability decision: IMMUTABLE.
/// An append-only log has no meaningful "upgrade" — its entire value is that its logic and past
/// entries never change.
///
/// Security boundary
/// ------------------
/// `recordDistribution` accepts only opaque identifiers and a hash/reference to off-chain metadata
/// (e.g. a hash of the distribution statement) — never bank details, payment instructions, or PII.
contract RwaShiftDistributionRegistry is AccessControl {
    bytes32 public constant DISTRIBUTION_AGENT_ROLE = keccak256("DISTRIBUTION_AGENT_ROLE");

    struct Distribution {
        address token;
        string distributionRefId;
        uint64 recordDate;
        uint256 totalAmountRef;
        bytes32 metadataHash;
        address recordedBy;
        uint256 recordedAt;
    }

    /// @dev token => distributionRefId => Distribution
    mapping(address => mapping(string => Distribution)) private _distributions;
    /// @dev token => list of distributionRefIds recorded for it, in recording order
    mapping(address => string[]) private _distributionRefsByToken;

    error ZeroAddress();
    error EmptyDistributionRef();
    error DistributionAlreadyRecorded(address token, string distributionRefId);
    error DistributionNotFound(address token, string distributionRefId);

    event DistributionRecorded(
        address indexed token,
        string distributionRefId,
        uint64 recordDate,
        uint256 totalAmountRef,
        bytes32 metadataHash,
        address indexed recordedBy
    );

    constructor(address platformAdmin) {
        if (platformAdmin == address(0)) revert ZeroAddress();
        _grantRole(DEFAULT_ADMIN_ROLE, platformAdmin);
        _grantRole(DISTRIBUTION_AGENT_ROLE, platformAdmin);
    }

    /// @param token The offering's ERC-3643 token this distribution pertains to.
    /// @param distributionRefId Opaque, off-chain-issued reference ID for this distribution run.
    /// @param recordDate Off-chain record/ex-date for the distribution (unix timestamp, day granularity is fine).
    /// @param totalAmountRef A reference amount (e.g. total distributed, in the offering's off-chain
    /// accounting unit). Not a token transfer — purely an auditable reference figure.
    /// @param metadataHash keccak256 (or similar) hash of the off-chain distribution statement/report,
    /// letting anyone verify a specific document matches what was recorded on-chain without the
    /// document itself ever touching the chain.
    function recordDistribution(
        address token,
        string calldata distributionRefId,
        uint64 recordDate,
        uint256 totalAmountRef,
        bytes32 metadataHash
    ) external onlyRole(DISTRIBUTION_AGENT_ROLE) {
        if (token == address(0)) revert ZeroAddress();
        if (bytes(distributionRefId).length == 0) revert EmptyDistributionRef();
        if (_distributions[token][distributionRefId].recordedAt != 0) {
            revert DistributionAlreadyRecorded(token, distributionRefId);
        }

        _distributions[token][distributionRefId] = Distribution({
            token: token,
            distributionRefId: distributionRefId,
            recordDate: recordDate,
            totalAmountRef: totalAmountRef,
            metadataHash: metadataHash,
            recordedBy: msg.sender,
            recordedAt: block.timestamp
        });
        _distributionRefsByToken[token].push(distributionRefId);

        emit DistributionRecorded(token, distributionRefId, recordDate, totalAmountRef, metadataHash, msg.sender);
    }

    function getDistribution(address token, string calldata distributionRefId)
        external
        view
        returns (Distribution memory distribution)
    {
        distribution = _distributions[token][distributionRefId];
        if (distribution.recordedAt == 0) revert DistributionNotFound(token, distributionRefId);
    }

    function distributionCount(address token) external view returns (uint256) {
        return _distributionRefsByToken[token].length;
    }

    function distributionRefAt(address token, uint256 index) external view returns (string memory) {
        return _distributionRefsByToken[token][index];
    }
}
