// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { RwaShiftTestBase } from "../utils/RwaShiftTestBase.sol";
import { RwaShiftDistributionRegistry } from "../../src/core/RwaShiftDistributionRegistry.sol";

contract RwaShiftDistributionRegistryTest is RwaShiftTestBase {
    // Mirrors RwaShiftDistributionRegistry.DistributionRecorded for vm.expectEmit (Solidity 0.8.17
    // does not support emitting another contract's qualified event name).
    event DistributionRecorded(
        address indexed token,
        string distributionRefId,
        uint64 recordDate,
        uint256 totalAmountRef,
        bytes32 metadataHash,
        address indexed recordedBy
    );

    address internal token = address(0xBEEF);

    function test_recordDistribution_storesEntryAndEmits() public {
        vm.prank(platformAdmin);
        vm.expectEmit(true, true, false, true, address(distributionRegistry));
        emit DistributionRecorded(token, "DIST-001", 1_700_000_000, 50_000, keccak256("statement"), platformAdmin);
        distributionRegistry.recordDistribution(token, "DIST-001", 1_700_000_000, 50_000, keccak256("statement"));

        RwaShiftDistributionRegistry.Distribution memory d = distributionRegistry.getDistribution(token, "DIST-001");
        assertEq(d.token, token);
        assertEq(d.totalAmountRef, 50_000);
        assertEq(d.recordedBy, platformAdmin);
        assertEq(distributionRegistry.distributionCount(token), 1);
        assertEq(distributionRegistry.distributionRefAt(token, 0), "DIST-001");
    }

    function test_recordDistribution_revertsForNonAgent() public {
        vm.prank(stranger);
        vm.expectRevert();
        distributionRegistry.recordDistribution(token, "DIST-002", 1, 1, bytes32(0));
    }

    function test_recordDistribution_revertsOnDuplicateRef() public {
        vm.startPrank(platformAdmin);
        distributionRegistry.recordDistribution(token, "DIST-003", 1, 1, bytes32(0));
        vm.expectRevert(
            abi.encodeWithSelector(RwaShiftDistributionRegistry.DistributionAlreadyRecorded.selector, token, "DIST-003")
        );
        distributionRegistry.recordDistribution(token, "DIST-003", 2, 2, bytes32(0));
        vm.stopPrank();
    }

    function test_recordDistribution_revertsOnZeroToken() public {
        vm.prank(platformAdmin);
        vm.expectRevert(RwaShiftDistributionRegistry.ZeroAddress.selector);
        distributionRegistry.recordDistribution(address(0), "DIST-004", 1, 1, bytes32(0));
    }

    function test_recordDistribution_revertsOnEmptyRef() public {
        vm.prank(platformAdmin);
        vm.expectRevert(RwaShiftDistributionRegistry.EmptyDistributionRef.selector);
        distributionRegistry.recordDistribution(token, "", 1, 1, bytes32(0));
    }

    function test_getDistribution_revertsForUnknownRef() public {
        vm.expectRevert(
            abi.encodeWithSelector(RwaShiftDistributionRegistry.DistributionNotFound.selector, token, "MISSING")
        );
        distributionRegistry.getDistribution(token, "MISSING");
    }

    function test_grantDistributionAgentRole_allowsRecording() public {
        address newAgent = makeAddr("newDistributionAgent");
        bytes32 distributionAgentRole = distributionRegistry.DISTRIBUTION_AGENT_ROLE();
        vm.prank(platformAdmin);
        distributionRegistry.grantRole(distributionAgentRole, newAgent);

        vm.prank(newAgent);
        distributionRegistry.recordDistribution(token, "DIST-005", 1, 1, bytes32(0));
        assertEq(distributionRegistry.distributionCount(token), 1);
    }
}
