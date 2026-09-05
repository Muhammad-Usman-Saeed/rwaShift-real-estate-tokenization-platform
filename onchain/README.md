# RWA Shift Real Estate — On-Chain Layer

ERC-3643 (T-REX) on-chain layer for **RWA Shift Real Estate**, the real-estate tokenization
product of the RWA Shift Platform. This repository implements only the **on-chain** system:
token issuance, transfer-restriction/eligibility enforcement, agent/role permissions, and a
minimal distribution audit trail. Everything else — property data, legal documents, KYC
documents, investor PII, offering workflow, payments, reporting — lives in Spring
Boot/MySQL and is intentionally kept off-chain and out of this repository.

## Domain model

The physical property is never the investment token. The chain only ever sees the investment
interest in the legal structure that owns the property:

```
Property → Legal Structure / SPV → Investment Offering → ERC-3643 investment units
```

Example: *Dubai Business Tower* ($10,000,000 value, owned by *Dubai Business Tower SPV Ltd.*)
raises a $2,000,000 offering of 20,000 units at $100/unit. A $10,000 investment mints 100
ERC-3643 units to the investor's wallet. The property, its valuation, and the SPV's legal
paperwork never touch the chain — only an opaque `offeringRefId` and the ERC-3643 parameters do.

## Repository layout

```
src/
├── core/
│   ├── RwaShiftTokenFactory.sol          # per-offering T-REX suite deployer
│   └── RwaShiftDistributionRegistry.sol  # append-only distribution audit trail
├── identity/
│   └── RwaShiftIdentityGateway.sol       # off-chain KYC → on-chain eligibility bridge
script/
├── lib/PlatformDeploy.sol   # shared one-time platform infra deployment
├── DeployLocal.s.sol        # deploy to Anvil
├── DeploySepolia.s.sol      # deploy to Sepolia (env-var driven)
└── SeedDemo.s.sol           # acceptance-scenario demo seed
test/
├── unit/         # RwaShiftTokenFactory, RwaShiftIdentityGateway, RwaShiftDistributionRegistry
├── integration/  # full offering lifecycle (issuance, transfers, freeze, recovery, auth)
├── fuzz/         # supply-cap and eligibility-gating fuzz tests
└── invariant/    # accounting/eligibility invariants under randomized action sequences
```

`src/token`, `src/compliance`, `src/interfaces`, `src/libraries`, `src/mocks` are intentionally
empty/absent: ERC-3643 itself, and every compliance module used by V1
(`SupplyLimitModule`), come from the audited [ERC-3643/ERC-3643](https://github.com/ERC-3643/ERC-3643)
reference implementation in `lib/T-REX`, composed rather than re-implemented. See
[Why so little custom Solidity](#why-so-little-custom-solidity) below.

## Architecture

### Why so little custom Solidity

Per the standards-driven mandate, this repo deliberately does **not** reimplement ERC-3643. It
depends on:

- **[`ERC-3643/ERC-3643`](https://github.com/ERC-3643/ERC-3643)** (`lib/T-REX`, v4.1.3) —
  `Token`, `IdentityRegistry`, `IdentityRegistryStorage`, `ClaimTopicsRegistry`,
  `TrustedIssuersRegistry`, `ModularCompliance`, `TREXFactory`, `TREXImplementationAuthority`, and
  the compliance module library (`SupplyLimitModule`, `CountryAllowModule`, `MaxBalanceModule`,
  etc.). This is the actively-maintained continuation of Tokeny's original `T-REX` reference
  implementation (same codebase, same audits, now maintained under the ERC-3643 Association org —
  `TokenySolutions/T-REX` itself is archived/deprecated). Installed under the local path `lib/T-REX`
  purely for import-path stability; the remote is `ERC-3643/ERC-3643`, not Tokeny's repo (see
  `.gitmodules`).
- **[`onchain-id/solidity`](https://github.com/onchain-id/solidity)** (`lib/solidity`, v2.2.1) —
  `Identity`, `IdFactory`, `ImplementationAuthority` (OnchainID, ERC-734/735 identities that
  ERC-3643 requires per investor).
- **OpenZeppelin Contracts v4.9.6** (pinned to match T-REX's own dependency) — `AccessControl`
  for the three RWA Shift-specific contracts below.

Three small contracts are RWA Shift-specific, each with a narrow, single responsibility:

| Contract | Responsibility |
|---|---|
| [`RwaShiftTokenFactory`](src/core/RwaShiftTokenFactory.sol) | Turns an approved offering into a deployed T-REX suite (CREATE2, salted by `offeringRefId`), and tracks the offering ⇄ token mapping. |
| [`RwaShiftIdentityGateway`](src/identity/RwaShiftIdentityGateway.sol) | The only path from an off-chain KYC decision to an on-chain, agent-gated `IdentityRegistry.registerIdentity`/`updateCountry`/`deleteIdentity` call. |
| [`RwaShiftDistributionRegistry`](src/core/RwaShiftDistributionRegistry.sol) | Append-only on-chain anchor for off-chain-computed distribution runs (reference ID, date, amount reference, statement hash). |

Everything else — issuance, transfer restriction, freeze/unfreeze, recovery, pause, agent
management, compliance modules — is T-REX's own protocol surface, used as-is.

### Deployment topology

One-time **platform infrastructure** (deployed by [`PlatformDeploy.sol`](script/lib/PlatformDeploy.sol),
shared by [`DeployLocal.s.sol`](script/DeployLocal.s.sol) and [`DeploySepolia.s.sol`](script/DeploySepolia.s.sol)):

```
T-REX implementation contracts (Token, CTR, TIR, IRS, IR, MC)
        │
        ▼
TREXImplementationAuthority  ── addAndUseTREXVersion(4.1.3, {...})
        │
        ▼
TREXFactory(implementationAuthority, idFactory) ──ownership──► RwaShiftTokenFactory

OnchainID Identity implementation ──► ImplementationAuthority ──► IdFactory ──ownership──► RwaShiftIdentityGateway

SupplyLimitModule (shared, initialized once)
RwaShiftDistributionRegistry (standalone)
```

Per-**offering** (via `RwaShiftTokenFactory.createOffering`, which calls the audited
`TREXFactory.deployTREXSuite` under the hood):

```
Token ──identityRegistry──► IdentityRegistry ──storage──► IdentityRegistryStorage
  │                              agents: [RwaShiftIdentityGateway]
  agents: [tokenAgent, ...]
  │
  compliance ──► ModularCompliance ──modules──► [SupplyLimitModule (shared, limit = offering units)]
  owner = issuerAdmin (for this offering)
```

### Role mapping

The product spec's conceptual roles map onto ERC-3643/T-REX's own permission primitives — no
parallel permission system is introduced:

| Spec role | On-chain mechanism | Holder |
|---|---|---|
| `PLATFORM_ADMIN` | `DEFAULT_ADMIN_ROLE` on `RwaShiftTokenFactory` / `RwaShiftDistributionRegistry`; `owner()` of `RwaShiftIdentityGateway`, `TREXImplementationAuthority`, `IdFactory` | Platform operations (multisig recommended for Sepolia/mainnet) |
| `ISSUER_ADMIN` | `ISSUER_ADMIN_ROLE` on `RwaShiftTokenFactory` (who may *create* offerings — narrow, cannot touch an already-deployed suite); `owner()` of the deployed T-REX suite (who controls *that* offering) | Platform's shared agent wallet creates the suite; ownership of the *resulting* suite is handed to the tokenizing organization's own wallet |
| `TOKEN_AGENT` | T-REX `Token.isAgent` (native `AgentRoleUpgradeable`) — mint/burn/freeze/pause/recovery | Each organization's own on-chain wallet, one per organization (`OrganizationWalletProvisioningService`) — not the platform's shared agent key, precisely so a leaked platform key cannot mint/burn/freeze any organization's tokens |
| `IDENTITY_AGENT` | `onlyAgent` on `RwaShiftIdentityGateway` (T-REX's non-upgradeable `AgentRole`) | Spring Boot's identity/KYC service account |
| `COMPLIANCE_AGENT` | `owner()` of the offering's `ModularCompliance` (T-REX's `ModularCompliance` is `Ownable`-only, no agent layer) — same principal as `ISSUER_ADMIN` for that offering in V1 | Issuer admin (or a dedicated compliance-ops wallet, by transferring `ModularCompliance` ownership independently, if ever required) |
| `DISTRIBUTION_AGENT` | `DISTRIBUTION_AGENT_ROLE` on `RwaShiftDistributionRegistry` | Spring Boot's distribution service account |

`OnchainID`'s `IdFactory.createIdentity` is `onlyOwner` (no native agent layer either), which is
exactly why `RwaShiftIdentityGateway` exists: it owns the `IdFactory` and re-exposes identity
creation through T-REX's `AgentRole`, giving `IDENTITY_AGENT` its own least-privilege role
distinct from `PLATFORM_ADMIN`.

### Compliance

V1 ships two enforcement layers, both from the audited T-REX protocol:

1. **Eligibility** — enforced unconditionally by every `Token` in `_transfer`/`transfer`/`mint`
   via `identityRegistry.isVerified(to)`. V1's `ClaimDetails` at offering creation is empty (no
   claim topics required), so `isVerified` reduces to "does this wallet have a registered
   `OnchainID` identity in this offering's `IdentityRegistry`" — i.e. "did Spring Boot's
   (simulated) KYC decision get relayed on-chain via `RwaShiftIdentityGateway`." This is the
   acceptance scenario from the product spec: an unregistered wallet cannot receive; once
   registered, it can.
2. **Supply cap** — enforced by binding T-REX's `SupplyLimitModule` (unmodified, shared across
   offerings, storage keyed by the calling `ModularCompliance` address) with the offering's unit
   count as its limit. Issuance can never exceed the offering's configured cap on-chain.

Country restrictions, investor classifications, max-ownership, max-investors, lock-ups, and
transfer windows are **not wired in V1** but require zero new Solidity — T-REX already ships
`CountryAllowModule`, `CountryRestrictModule`, `MaxBalanceModule`, `TimeTransfersLimitsModule`,
etc. (`lib/T-REX/contracts/compliance/modular/modules/`). Binding one is a
`RwaShiftTokenFactory.createOffering` parameter (`complianceModules`/`complianceSettings`), not a
code change.

## Contract reference

### `RwaShiftTokenFactory`

- **Responsibility**: the only entry point that turns an approved offering into a deployed
  ERC-3643 suite.
- **Trust assumptions**: must own the platform's `TREXFactory`. `ISSUER_ADMIN_ROLE` gates who may
  request new suites; control over an already-deployed suite belongs solely to the `issuerAdmin`
  address passed into `createOffering` (the T-REX suite owner), not to this contract.
- **Upgradeability**: **immutable**. No user funds, narrow responsibility; the parts that
  legitimately need upgradeability (T-REX's implementation contracts) already get it via
  `TREXImplementationAuthority`.
- **Security boundary**: `createOffering` never accepts property metadata, valuations, legal
  documents, or PII — only `offeringRefId` (opaque) and ERC-3643 parameters.

| Function | Access | Type |
|---|---|---|
| `createOffering(offeringRefId, name, symbol, decimals, issuerAdmin, identityAgents, tokenAgents, complianceModules, complianceSettings)` | `ISSUER_ADMIN_ROLE` | tx |
| `recoverContractOwnership(contractAddress, newOwner)` | `DEFAULT_ADMIN_ROLE` | tx |
| `tokenForOffering(offeringRefId)` | public | read |
| `offeringForToken(token)` | public | read |
| `grantRole` / `revokeRole` (inherited `AccessControl`) | `DEFAULT_ADMIN_ROLE` | tx |

Event: `OfferingTokenCreated(string offeringRefId, address indexed token, address indexed issuerAdmin, string name, string symbol, uint8 decimals, address identityRegistry, address compliance)`

### `RwaShiftIdentityGateway`

- **Responsibility**: the only path from an off-chain (simulated) KYC decision to an on-chain
  registered/eligible investor identity.
- **Trust assumptions**: owns the platform `IdFactory`; must be an `IdentityRegistry` agent on
  every offering that uses it (wired automatically by `createOffering`). `onlyAgent` gates every
  state-changing function — investors and issuers never call it directly.
- **Upgradeability**: **immutable**. Pure coordination logic over two independently upgradeable,
  already-audited systems.
- **Security boundary**: never accepts KYC documents, PII, or claim payloads — only a wallet
  address, a deterministic salt, and an ISO-3166 country code (the minimum ERC-3643 itself
  requires on-chain).

| Function | Access | Type |
|---|---|---|
| `registerInvestorIdentity(identityRegistry, wallet, country, salt)` | `onlyAgent` | tx |
| `updateInvestorCountry(identityRegistry, wallet, country)` | `onlyAgent` | tx |
| `revokeInvestorIdentity(identityRegistry, wallet)` | `onlyAgent` | tx |
| `identityOf(wallet)` | public | read |
| `addAgent` / `removeAgent` (inherited `AgentRole`) | `onlyOwner` | tx |

Events: `InvestorIdentityRegistered(address indexed identityRegistry, address indexed wallet, address indexed identity, uint16 country)`,
`InvestorIdentityUpdated(address indexed identityRegistry, address indexed wallet, uint16 country)`,
`InvestorIdentityRevoked(address indexed identityRegistry, address indexed wallet)`

### `RwaShiftDistributionRegistry`

- **Responsibility**: append-only on-chain anchor for off-chain-computed distribution runs.
- **Trust assumptions**: `DISTRIBUTION_AGENT_ROLE` holders only. No update/delete — a correction
  is a new distribution reference, exactly like a ledger correcting entry.
- **Upgradeability**: **immutable**. An append-only log's entire value is that it never changes.
- **Security boundary**: only opaque identifiers, a date, a reference amount, and a hash of the
  off-chain statement — never bank details, payment instructions, or PII.

| Function | Access | Type |
|---|---|---|
| `recordDistribution(token, distributionRefId, recordDate, totalAmountRef, metadataHash)` | `DISTRIBUTION_AGENT_ROLE` | tx |
| `getDistribution(token, distributionRefId)` | public | read |
| `distributionCount(token)` / `distributionRefAt(token, index)` | public | read |

Event: `DistributionRecorded(address indexed token, string distributionRefId, uint64 recordDate, uint256 totalAmountRef, bytes32 metadataHash, address indexed recordedBy)`

### T-REX `Token` surface used directly (per offering)

Spring Boot calls these directly against the offering's `Token` address (from
`OfferingTokenCreated`/`tokenForOffering`) — no RWA Shift wrapper needed, since T-REX's own
access control (`onlyAgent`/`onlyOwner`) already provides the required least-privilege gating.

| Function | Access | Type | Notes |
|---|---|---|---|
| `mint(to, amount)` | Token agent | tx | Reverts if `to` not eligible, amount is zero, or the supply cap module rejects it |
| `burn(userAddress, amount)` | Token agent | tx | |
| `setAddressFrozen(userAddress, freeze)` | Token agent | tx | Full wallet freeze |
| `freezePartialTokens` / `unfreezePartialTokens` | Token agent | tx | Partial balance freeze |
| `pause` / `unpause` | Token agent | tx | Tokens deploy paused; agent must `unpause` before the first mint |
| `recoveryAddress(lostWallet, newWallet, investorOnchainID)` | Token agent | tx | Requires `newWallet` already hold a management key on `investorOnchainID` |
| `transfer(to, amount)` | Any holder | tx | Reverts unless `to` is verified, neither party frozen, and compliance passes |
| `balanceOf(address)` | public | read | |
| `totalSupply()` | public | read | |
| `isFrozen(address)` / `getFrozenTokens(address)` | public | read | |
| `identityRegistry()` / `compliance()` | public | read | Per-offering `IdentityRegistry`/`ModularCompliance` addresses |
| `addAgent(address)` / `isAgent(address)` | Token owner (issuer admin) | tx/read | |

`IdentityRegistry.isVerified(address)` / `investorCountry(address)` / `contains(address)` are the
read-side eligibility checks (`canTransfer()` equivalent for Spring Boot's UI/pre-flight checks).

## Local development (Anvil)

```bash
# terminal 1
anvil

# terminal 2 — one-time platform deployment
forge script script/DeployLocal.s.sol:DeployLocal --rpc-url anvil --broadcast

# seeds a demo offering and walks the full acceptance scenario:
#   create offering -> register Investor A only -> issue 100 units to A
#   -> A to unverified B fails -> verify B -> A to B succeeds
forge script script/SeedDemo.s.sol:SeedDemo --rpc-url anvil --broadcast
```

`deployments/local.json` is written by `DeployLocal` with every platform contract address; reuse
it for `SeedDemo` or for pointing a local Spring Boot instance at the chain.

## Sepolia deployment

```bash
cp .env.example .env   # fill in SEPOLIA_RPC_URL, DEPLOYER_PRIVATE_KEY, ETHERSCAN_API_KEY
source .env

forge script script/DeploySepolia.s.sol:DeploySepolia \
  --rpc-url sepolia --broadcast --verify -vvvv
```

Writes `deployments/sepolia.json` (chain ID + every contract address) and, via `--broadcast`,
`broadcast/DeploySepolia.s.sol/11155111/run-latest.json` (full transaction receipts/hashes).
`.env` is git-ignored; never commit private keys or RPC secrets.

## Testing

```bash
forge build
forge test              # unit + integration + fuzz + invariant
forge coverage           # coverage report
```

52 tests across four suites:

- **`test/unit/`** — `RwaShiftTokenFactory`, `RwaShiftIdentityGateway`, `RwaShiftDistributionRegistry`
  in isolation: access control, zero-address/duplicate/empty-input validation, event emission.
- **`test/integration/`** — full offering lifecycle: mint/burn authorization, eligibility-gated
  transfers, freeze/unfreeze (full and partial), recovery (with and without a valid management
  key), compliance/agent-management authorization boundaries.
- **`test/fuzz/`** — supply-cap enforcement never exceeded for any minted amount (single or
  sequential mints), transfer success/failure gated purely by recipient eligibility, frozen
  wallets can never transfer, for randomized amounts.
- **`test/invariant/`** — under randomized sequences of mint/transfer/freeze/burn across five
  eligible investors and one deliberately-never-registered investor: total supply never exceeds
  the on-chain cap, sum of balances always equals total supply, the ineligible investor's balance
  is always zero, and frozen amount never exceeds balance.

## Security notes

- **No custom cryptography, no `tx.origin` auth, no `delegatecall` to untrusted targets, no
  arbitrary external execution, no `selfdestruct`.** The only `delegatecall`s in the system are
  T-REX's own audited proxy pattern (`TokenProxy`, `IdentityRegistryProxy`, etc.), not introduced
  by RWA Shift code.
- **Checks-effects-interactions**: RWA Shift's own contracts perform their state write, then a
  single external call (to `TREXFactory`, `IIdFactory`, or T-REX's `IdentityRegistry`), and are
  not otherwise reentrant-sensitive (no value transfers, no callback surface).
- **Least privilege**: every privileged action is gated by a role/agent check scoped to exactly
  the actors who need it (see [Role mapping](#role-mapping)); no role's functions are reachable by
  `PLATFORM_ADMIN` bypassing the intended agent (e.g. platform admin cannot mint tokens directly —
  only a token agent can).
- **No PII/documents/valuations on-chain**, anywhere in the three RWA Shift contracts — verified
  by inspection of every function signature in this repo (opaque IDs, addresses, country codes,
  hashes, and amounts only).
- Compiled with `via_ir = true` (required — the factory's `createOffering` has too many locals for
  the legacy codegen path) and Solidity `0.8.17` pinned to match T-REX's fixed pragma.

## ABI / deployment-artifact export for Spring Boot (Web3j)

After `forge build`, every contract's ABI is at `out/<Contract>.sol/<Contract>.json` (`abi` key).
Contracts Spring Boot needs directly:

- `out/RwaShiftTokenFactory.sol/RwaShiftTokenFactory.json`
- `out/RwaShiftIdentityGateway.sol/RwaShiftIdentityGateway.json`
- `out/RwaShiftDistributionRegistry.sol/RwaShiftDistributionRegistry.json`
- `out/Token.sol/Token.json` (T-REX, via `lib/T-REX/contracts/token/Token.sol`) — used against
  each offering's deployed address
- `out/IdentityRegistry.sol/IdentityRegistry.json` (T-REX) — read-side eligibility checks

Generate Web3j Java wrappers directly from these ABIs, e.g.:

```bash
web3j generate solidity \
  -a out/RwaShiftTokenFactory.sol/RwaShiftTokenFactory.json \
  -o spring-boot/src/main/java -p com.rwashift.chain.contracts
```

Deployment addresses + chain ID for Spring Boot's config come from `deployments/local.json` /
`deployments/sepolia.json` (written by the deploy scripts) and, per-offering, from the
`OfferingTokenCreated` event (`tokenForOffering(offeringRefId)` is also queryable directly).
Transaction hashes for every platform-deployment transaction are in
`broadcast/<Script>.s.sol/<chainId>/run-latest.json`.

**Confirmation expectations**: treat 1 confirmation as sufficient on Anvil/local; on Sepolia,
Spring Boot should wait for the same finality depth it uses for any other Sepolia write (this
repo does not prescribe a chain-specific value — configure it alongside the RPC endpoint).
