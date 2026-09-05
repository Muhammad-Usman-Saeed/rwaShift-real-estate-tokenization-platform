// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { Script, console2 } from "forge-std/Script.sol";
import { PlatformDeploy } from "./lib/PlatformDeploy.sol";

/// @notice Deploys the full RWA Shift on-chain platform to Sepolia. All configuration comes from
/// environment variables — nothing is hardcoded or committed.
///
/// Required env vars:
///   SEPOLIA_RPC_URL        RPC endpoint (also referenced by foundry.toml's [rpc_endpoints])
///   DEPLOYER_PRIVATE_KEY   private key of the deploying/broadcasting account (funded with Sepolia ETH)
///   ETHERSCAN_API_KEY      for `--verify` (optional but recommended)
///
/// Optional env vars:
///   PLATFORM_ADMIN         address to receive PLATFORM_ADMIN control; defaults to the deployer address
///
/// Usage:
///   forge script script/DeploySepolia.s.sol:DeploySepolia \
///     --rpc-url sepolia --broadcast --verify -vvvv
contract DeploySepolia is Script {
    function run() external returns (PlatformDeploy.Deployment memory d) {
        uint256 deployerKey = vm.envUint("DEPLOYER_PRIVATE_KEY");
        address deployer = vm.addr(deployerKey);
        address platformAdmin = vm.envOr("PLATFORM_ADMIN", deployer);

        vm.startBroadcast(deployerKey);
        d = PlatformDeploy.run(platformAdmin);
        vm.stopBroadcast();

        console2.log("chainId                          ", block.chainid);
        console2.log("PLATFORM_ADMIN                    ", platformAdmin);
        console2.log("Token implementation               ", d.tokenImplementation);
        console2.log("ClaimTopicsRegistry impl            ", d.ctrImplementation);
        console2.log("TrustedIssuersRegistry impl         ", d.tirImplementation);
        console2.log("IdentityRegistryStorage impl        ", d.irsImplementation);
        console2.log("IdentityRegistry impl               ", d.irImplementation);
        console2.log("ModularCompliance impl              ", d.mcImplementation);
        console2.log("TREXImplementationAuthority          ", d.trexImplementationAuthority);
        console2.log("OnchainID Identity impl              ", d.identityImplementation);
        console2.log("OnchainID ImplementationAuthority     ", d.identityImplementationAuthority);
        console2.log("OnchainID IdFactory                   ", d.idFactory);
        console2.log("TREXFactory                           ", d.trexFactory);
        console2.log("RwaShiftTokenFactory                  ", d.rwaShiftTokenFactory);
        console2.log("RwaShiftIdentityGateway               ", d.rwaShiftIdentityGateway);
        console2.log("RwaShiftDistributionRegistry          ", d.rwaShiftDistributionRegistry);
        console2.log("SupplyLimitModule (shared)            ", d.supplyLimitModule);

        _writeDeploymentJson(d, platformAdmin);
    }

    function _writeDeploymentJson(PlatformDeploy.Deployment memory d, address platformAdmin) internal {
        string memory json = "deployment";
        vm.serializeUint(json, "chainId", block.chainid);
        vm.serializeAddress(json, "platformAdmin", platformAdmin);
        vm.serializeAddress(json, "tokenImplementation", d.tokenImplementation);
        vm.serializeAddress(json, "ctrImplementation", d.ctrImplementation);
        vm.serializeAddress(json, "tirImplementation", d.tirImplementation);
        vm.serializeAddress(json, "irsImplementation", d.irsImplementation);
        vm.serializeAddress(json, "irImplementation", d.irImplementation);
        vm.serializeAddress(json, "mcImplementation", d.mcImplementation);
        vm.serializeAddress(json, "trexImplementationAuthority", d.trexImplementationAuthority);
        vm.serializeAddress(json, "identityImplementation", d.identityImplementation);
        vm.serializeAddress(json, "identityImplementationAuthority", d.identityImplementationAuthority);
        vm.serializeAddress(json, "idFactory", d.idFactory);
        vm.serializeAddress(json, "trexFactory", d.trexFactory);
        vm.serializeAddress(json, "rwaShiftTokenFactory", d.rwaShiftTokenFactory);
        vm.serializeAddress(json, "rwaShiftIdentityGateway", d.rwaShiftIdentityGateway);
        vm.serializeAddress(json, "rwaShiftDistributionRegistry", d.rwaShiftDistributionRegistry);
        string memory finalJson = vm.serializeAddress(json, "supplyLimitModule", d.supplyLimitModule);

        vm.writeJson(finalJson, "./deployments/sepolia.json");
        console2.log("Wrote deployments/sepolia.json");
    }
}
