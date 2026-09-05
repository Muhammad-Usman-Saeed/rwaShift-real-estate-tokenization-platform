// SPDX-License-Identifier: MIT
pragma solidity 0.8.17;

import { Script } from "forge-std/Script.sol";

import { ClaimTopicsRegistry } from "@trex/registry/implementation/ClaimTopicsRegistry.sol";
import { TrustedIssuersRegistry } from "@trex/registry/implementation/TrustedIssuersRegistry.sol";
import { IdentityRegistryStorage } from "@trex/registry/implementation/IdentityRegistryStorage.sol";
import { IdentityRegistry } from "@trex/registry/implementation/IdentityRegistry.sol";
import { ModularCompliance } from "@trex/compliance/modular/ModularCompliance.sol";
import { Token } from "@trex/token/Token.sol";
import { TREXImplementationAuthority } from "@trex/proxy/authority/TREXImplementationAuthority.sol";
import { ITREXImplementationAuthority } from "@trex/proxy/authority/ITREXImplementationAuthority.sol";
import { TREXFactory } from "@trex/factory/TREXFactory.sol";
import { SupplyLimitModule } from "@trex/compliance/modular/modules/SupplyLimitModule.sol";

import { Identity } from "@onchain-id/solidity/contracts/Identity.sol";
import {
    ImplementationAuthority as OnchainIdImplementationAuthority
} from "@onchain-id/solidity/contracts/proxy/ImplementationAuthority.sol";
import { IdFactory } from "@onchain-id/solidity/contracts/factory/IdFactory.sol";

import { RwaShiftTokenFactory } from "../../src/core/RwaShiftTokenFactory.sol";
import { RwaShiftIdentityGateway } from "../../src/identity/RwaShiftIdentityGateway.sol";
import { RwaShiftDistributionRegistry } from "../../src/core/RwaShiftDistributionRegistry.sol";

/// @notice Deploys the one-time, platform-wide infrastructure: the audited T-REX implementation
/// contracts + TREXImplementationAuthority + TREXFactory, the OnchainID Identity implementation +
/// factory, and the three RWA Shift-specific contracts wired on top of them.
///
/// This is infrastructure setup, not a contract in its own right, so it lives under `script/` as a
/// reusable library shared by `DeployLocal.s.sol` and `DeploySepolia.s.sol` rather than being
/// duplicated between them.
library PlatformDeploy {
    struct Deployment {
        address tokenImplementation;
        address ctrImplementation;
        address tirImplementation;
        address irsImplementation;
        address irImplementation;
        address mcImplementation;
        address trexImplementationAuthority;
        address identityImplementation;
        address identityImplementationAuthority;
        address idFactory;
        address trexFactory;
        address rwaShiftTokenFactory;
        address rwaShiftIdentityGateway;
        address rwaShiftDistributionRegistry;
        address supplyLimitModule;
    }

    /// @param platformAdmin becomes PLATFORM_ADMIN (DEFAULT_ADMIN_ROLE + initial ISSUER_ADMIN_ROLE
    /// on RwaShiftTokenFactory, owner of the identity gateway / distribution registry via their
    /// respective admin roles, and owner of every platform-level contract deployed here).
    function run(address platformAdmin) internal returns (Deployment memory d) {
        // --- T-REX implementation contracts (deployed once, referenced by proxies via the IA) ---
        d.tokenImplementation = address(new Token());
        d.ctrImplementation = address(new ClaimTopicsRegistry());
        d.tirImplementation = address(new TrustedIssuersRegistry());
        d.irsImplementation = address(new IdentityRegistryStorage());
        d.irImplementation = address(new IdentityRegistry());
        d.mcImplementation = address(new ModularCompliance());

        // --- OnchainID: one shared Identity implementation + IA + IdFactory for all investors ---
        // `isLibrary = true` prevents this deployment from ever being initialized as a live identity.
        d.identityImplementation = address(new Identity(platformAdmin, true));
        d.identityImplementationAuthority = address(new OnchainIdImplementationAuthority(d.identityImplementation));
        d.idFactory = address(new IdFactory(d.identityImplementationAuthority));

        // --- T-REX implementation authority (reference IA) + version registration ---
        TREXImplementationAuthority ia = new TREXImplementationAuthority(true, address(0), address(0));
        ITREXImplementationAuthority.Version memory version =
            ITREXImplementationAuthority.Version({ major: 4, minor: 1, patch: 3 });
        ITREXImplementationAuthority.TREXContracts memory contracts_ = ITREXImplementationAuthority.TREXContracts({
            tokenImplementation: d.tokenImplementation,
            ctrImplementation: d.ctrImplementation,
            irImplementation: d.irImplementation,
            irsImplementation: d.irsImplementation,
            tirImplementation: d.tirImplementation,
            mcImplementation: d.mcImplementation
        });
        ia.addAndUseTREXVersion(version, contracts_);
        d.trexImplementationAuthority = address(ia);

        // --- TREXFactory: deploys per-offering suites via CREATE2 ---
        TREXFactory trexFactory = new TREXFactory(d.trexImplementationAuthority, d.idFactory);
        d.trexFactory = address(trexFactory);
        IdFactory(d.idFactory).addTokenFactory(d.trexFactory);

        // --- RWA Shift application layer ---
        RwaShiftTokenFactory tokenFactory = new RwaShiftTokenFactory(d.trexFactory, platformAdmin);
        d.rwaShiftTokenFactory = address(tokenFactory);
        // Hand TREXFactory ownership to RwaShiftTokenFactory so all future suite deployments must
        // go through it (and therefore through its ISSUER_ADMIN_ROLE gate).
        trexFactory.transferOwnership(d.rwaShiftTokenFactory);

        RwaShiftIdentityGateway identityGateway = new RwaShiftIdentityGateway(d.idFactory);
        d.rwaShiftIdentityGateway = address(identityGateway);
        // The gateway must own the IdFactory to be able to create investor OnchainIDs.
        IdFactory(d.idFactory).transferOwnership(d.rwaShiftIdentityGateway);
        identityGateway.addAgent(platformAdmin);
        // Ownable's deployer-is-owner default may not be `platformAdmin` if a separate deployer
        // key broadcasts this script; align ownership explicitly.
        identityGateway.transferOwnership(platformAdmin);

        RwaShiftDistributionRegistry distributionRegistry = new RwaShiftDistributionRegistry(platformAdmin);
        d.rwaShiftDistributionRegistry = address(distributionRegistry);

        // Shared, audited T-REX compliance module (reused as-is, not re-implemented). Its storage
        // is keyed by the calling ModularCompliance address, so a single deployed instance can be
        // bound, with an offering-specific limit, into every offering's compliance. Used to
        // enforce on-chain that issuance can never exceed the offering's configured unit cap.
        // (ERC-3643/ERC-3643's modules are plain, non-upgradeable contracts — no initializer.)
        SupplyLimitModule supplyLimitModule = new SupplyLimitModule();
        d.supplyLimitModule = address(supplyLimitModule);
    }
}
