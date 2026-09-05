# ADR-012: Smart Contract Upgradeability Policy

## Status

Accepted

## Context

The on-chain layer mixes upgradeable and immutable contracts: T-REX's own protocol contracts
(`Token`, `IdentityRegistry`, etc.) are upgradeable via `TREXImplementationAuthority`'s proxy
pattern (an audited, standard mechanism), while the three RWA Shift-specific contracts
(`RwaShiftTokenFactory`, `RwaShiftIdentityGateway`, `RwaShiftDistributionRegistry`) were built as
plain, non-upgradeable contracts. The off-chain platform needs a clear, stated policy for which is
which, since it determines how the platform must react to a contract-level bug or change.

## Decision

- **T-REX protocol contracts**: upgradeable, via the audited `TREXImplementationAuthority`
  mechanism already built into T-REX. The off-chain platform never needs to redeploy or
  re-point addresses for these — an upgrade happens at the implementation-authority level and
  every existing deployed suite picks it up transparently.
- **The three RWA Shift-specific contracts**: deliberately **immutable**, per-contract rationale
  in `onchain/README.md`'s [Contract reference](../../../onchain/README.md#contract-reference)
  section — each holds no user funds and has a narrow, single responsibility, so the operational
  cost of "redeploy and re-point config" on the rare occasion a bug is found is judged lower than
  the risk surface an upgrade mechanism (proxy admin key, storage-layout discipline) would add to
  contracts this small.
- **Off-chain consequence**: `rwashift.blockchain.token-factory-address`,
  `identity-gateway-address`, and `distribution-registry-address` are runtime configuration
  (`application.yml` / environment variables), never hardcoded, precisely so that if one of the
  three RWA Shift-specific contracts is ever redeployed (bug fix, not a live "upgrade"), the
  platform only needs a config change and restart — no code change, no migration of in-flight
  `BlockchainTransaction`/`ContractDeployment` rows tied to the *old* address, since those remain
  valid historical records of transactions against that address.

## Consequences

- A bug found in `RwaShiftTokenFactory` after some offerings are already deployed does not affect
  those existing offerings' already-deployed T-REX suites (the factory only orchestrates
  *creation*; it holds no ongoing authority over an already-deployed suite, which is owned by that
  offering's own `issuerAdmin`). A new `RwaShiftTokenFactory` deployment only affects *future*
  `createOffering` calls.
- `RwaShiftIdentityGateway` and `RwaShiftDistributionRegistry` do hold ongoing authority (agent
  role on every `IdentityRegistry`; the sole writer of distribution records). Redeploying either
  requires re-granting the new contract's agent/role status on every existing offering — an
  operational runbook step, not an automatic consequence of a config change. Not automated in V1;
  flagged here as the operational cost of the "immutable, no proxy" choice for these two contracts
  specifically.
- `ContractDeployment` rows in MySQL record the exact address used at deployment time for every
  offering, so historical on-chain interactions remain traceable even after a config change points
  new deployments at a different factory/gateway address.
