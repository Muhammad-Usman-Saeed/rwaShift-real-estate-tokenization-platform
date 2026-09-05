# RWA Shift — QA Testing Guide

## 1. Environment setup

```bash
cp .env.example .env
docker compose up --build
```

This gives you, all wired together automatically: MySQL, an Anvil local blockchain, the on-chain
contracts deployed to it, the Spring Boot API + OAuth2 server (`:8080`), and the Next.js frontend
(`:3000`). Demo data seeds automatically on first boot (`RWASHIFT_SEED_ENABLED=true` by default).

**Known environment gotcha**: `docker compose up --build` re-runs the on-chain contract deployment
script every time (it's a one-shot service with no idempotency check), which redeploys fresh
contracts on every rebuild. If the running backend container isn't also restarted afterward, it
keeps pointing at the *old* contract addresses until it is. If tokenization/on-chain actions start
behaving strangely after a partial rebuild, restart the `app` container.

**Known environment gotcha #2 — organization wallets start unfunded**: each organization gets its
own on-chain wallet the first time it tokenizes an offering (see §4.4). On Anvil that wallet has
zero test ETH until someone sends it some, so the first offering an organization ever tokenizes
will predictably get stuck in `Tokenizing` — this is expected, not a bug, and Platform Admin →
Tokenization tells you exactly which wallet needs funding and gives you a one-click Retry once it's
funded.

Swagger UI for the raw API: `http://localhost:8080/swagger-ui.html`.

## 2. Seed accounts

All demo passwords are `ChangeMe123!`.

| Email | Role | Notes |
|---|---|---|
| `platform-admin@rwashift.com` | Platform Admin | Full cross-organization access |
| `issuer-admin@abcrealestate.com` | Issuer Admin (ABC Real Estate) | Manages assets/offerings for one org |
| `compliance@abcrealestate.com` | Compliance Officer (ABC Real Estate) | Approves offerings, reviews KYC |
| `investor-a@example.com` | Investor | Wallet `0x70997970C51812dc3A010C7d01b50e0d17dc79C8` (Anvil account #1) — KYC-verified, already holds a settled investment |
| `investor-b@example.com` | Investor | Wallet `0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC` (Anvil account #2) — deliberately left KYC-unverified for testing the "not eligible" path |

Both investor wallets are Anvil's well-known local test accounts, pre-funded with test ETH and
(as of the crypto-payment feature) 1,000,000 mock USDC each — see §7.

Additional organizations/users can be created live via Platform Admin → Organizations → *Manage
Users*, which generates a one-time password shown once on screen.

## 3. Roles and what each can do

| Role | Can do |
|---|---|
| `PLATFORM_ADMIN` | Everything: create/delete organizations, provision org users, approve offerings, review KYC, view all data across every organization |
| `ORGANIZATION_ADMIN` | Same as Issuer Admin, plus can manage org users within their own org |
| `ISSUER_ADMIN` / `ISSUER_OPERATOR` | Create/edit assets, legal structures, offerings (their own org only); cannot approve their own offerings |
| `COMPLIANCE_OFFICER` | Approve/reject offerings, review/verify/reject KYC, view audit trail |
| `INVESTOR` | Browse open offerings, invest, manage their own profile/wallet, view their own portfolio |

A user can hold roles in multiple organizations at once via separate memberships; `PLATFORM_ADMIN`
is the only fully global role.

## 4. Core workflows to test

### 4.1 Organization → Asset → Legal Structure → Offering → Approval → Tokenization
The full issuer-side pipeline described in [`user-guide-happy-path.md`](user-guide-happy-path.md).
Test both the happy path and:
- Editing an asset/offering/legal structure — only allowed while status is `DRAFT` (or
  `UNDER_VERIFICATION` for assets); confirm the edit UI/API rejects changes once verified/approved.
- Submitting an offering for review without required fields.
- Compliance **rejecting** an offering (not just approving) — confirm it lands in `REJECTED` and
  the issuer sees a clear "needs revision" state, not a dead end.
- Deleting an organization that has assets attached — should be blocked with a clear message, not
  a raw 500/constraint error.

### 4.2 Investor onboarding (KYC + wallet)
- Investor with `NOT_STARTED` KYC attempting to invest → should be blocked with a clear
  call-to-action, not a confusing error.
- KYC `SUBMITTED` → `UNDER_REVIEW` → `VERIFIED` and → `REJECTED` paths, both from the Admin KYC
  Reviews screen.
- Wallet address validation — must be a well-formed `0x…` address; duplicate wallet addresses
  across investors should be rejected.

### 4.3 Investment + payment (the highest-risk area — test thoroughly)
Two independent payment rails, each with its own confirmation mechanism:

- **Bank Transfer**: `PAYMENT_PENDING` until a Platform Admin manually clicks "Confirm Payment."
  Verify a `PAYMENT_FAILED` path is reachable too (admin-driven).
- **Crypto Wallet**: the investor pays through `RwaShiftPaymentRouter.payInvestment(investmentId,
  amount)` — an `approve` then a tagged call, not a bare USDC `transfer` — so a backend watcher
  (`CryptoPaymentWatcher`) detects the matching `PaymentReceived` event and settles the *exact*
  investment automatically, with zero admin involvement and zero ambiguity (the investment id is
  baked into the on-chain event, not guessed from wallet+amount). To test this without a real
  wallet extension, simulate the investor's payment directly with Foundry's `cast`:
  ```bash
  cast send <USDC_TOKEN_ADDRESS> "approve(address,uint256)" <PAYMENT_ROUTER_ADDRESS> <AMOUNT*1e6> \
    --private-key <INVESTOR_WALLET_PRIVATE_KEY> --rpc-url http://127.0.0.1:8545
  cast send <PAYMENT_ROUTER_ADDRESS> "payInvestment(string,uint256)" <INVESTMENT_ID> <AMOUNT*1e6> \
    --private-key <INVESTOR_WALLET_PRIVATE_KEY> --rpc-url http://127.0.0.1:8545
  ```
  Get the current `USDC_TOKEN_ADDRESS`/`PAYMENT_ROUTER_ADDRESS` from the `app` container's boot
  logs (`docker compose logs app | grep PAYMENT_ROUTER_ADDRESS`) — they're freshly deployed on
  every stack rebuild, not fixed values. `INVESTMENT_ID` is the investment's own ULID (visible in
  its URL/API response).
- **Wrong-investment sanity check**: calling `payInvestment` with an investment id that doesn't
  exist, or one that isn't `PAYMENT_PENDING`/`CRYPTO_WALLET`, must be logged and ignored by the
  watcher — never silently confirm the wrong (or no) investment. Worth a deliberate negative test.
- **Cancel**: an investor can self-cancel while `ELIGIBILITY_PENDING` or `PAYMENT_PENDING`, but
  *not* once payment is `CONFIRMED` — confirm the cancel button/endpoint actually disappears/403s
  past that point.
- **Resume payment**: navigating away from a pending crypto payment and returning via "Resume
  Payment" (Portfolio or Investments list) must show the *same* deposit instructions, not create a
  duplicate investment.
- **Idempotency**: retrying the same "Confirm Investment" click (e.g. on network hiccup) should not
  create a duplicate investment — the API is idempotency-keyed per attempt.

### 4.4 Tokenization / on-chain state
- After an offering tokenizes, cross-check the **on-chain** token balance (shown in
  Portfolio → On-Chain Holdings, or read directly via a block explorer / `cast call`) against the
  **application record** — they should always agree. A mismatch is a serious bug.
- A reverted/failed blockchain transaction (e.g. gas issue) should mark the relevant
  `ContractDeployment`/`BlockchainTransaction` as `FAILED`, not leave it stuck `PENDING` forever.
- **Organization wallets and funding**: each organization signs its own token-authority
  transactions (`unpause`, `mint`) with its own on-chain wallet, not the platform's — see
  [`critical-analysis.md`](critical-analysis.md) for why. Platform Admin → Tokenization lists every
  organization's wallet address and live gas balance with a "Needs funding" badge; fund it with
  `cast send <WALLET_ADDRESS> --value 1ether --private-key <ANY_FUNDED_ANVIL_KEY> --rpc-url
  http://127.0.0.1:8545` on local/Anvil. Platform Admin → Blockchain Transactions has a **Retry**
  button on the failed `unpause` row once the wallet is funded — confirm the offering actually
  reaches `Open` after a retry, not just that the retry call returns success.

### 4.5 Notifications, News, Activity
- The notification bell and news ticker are **derived, not stored** — they recompute from current
  data on every load. After acting on something (e.g. approving an offering), confirm the relevant
  notification actually disappears rather than needing a manual dismiss.
- "Mark all as read" state is per-browser (localStorage), not per-account — logging in on a
  different browser will show items as unread again. This is expected, not a bug.

### 4.6 Wallet identity — one wallet, one account
A wallet address can reach the platform two ways: signing in with it directly ("Sign in with
Wallet"), or linking it to an already-logged-in investor's profile. Both must always resolve to the
*same* account for the *same* address — worth testing deliberately, since each of these was a real
bug found and fixed:
- Sign in with a wallet that's already linked to an existing investor (via onboarding or profile
  "Link Wallet") → must land on that same account, never create a second one.
- From a different account, try to "Link Wallet" with an address that's already used to sign in
  elsewhere → must be rejected (`422`, "already used to sign in to a different account").
- An investor switches their linked wallet from X to Y, then later signs in with the *old* wallet X
  → must still resolve back to that same investor, not create an orphan account.
- An investor switches back to a wallet (X) they'd previously used and moved away from → must
  succeed cleanly (`200`), not be rejected as "belongs to another investor."
- From a genuinely different investor, try to link a wallet that's currently someone else's active
  primary wallet → must still be rejected.

## 5. Negative / edge cases worth deliberately trying

- Submitting an offering amount that isn't an exact multiple of the unit price.
- Investing an amount below the offering's stated minimum.
- Investing more units than remain available.
- Two investors' crypto payments landing in the same on-chain block.
- An expired/refreshed session mid-action (the app has automatic token refresh — force a refresh
  failure by restarting the backend mid-session and confirm the user is cleanly signed out to
  `/login`, not left in a broken state).
- Navigating directly to a portal route for a role you don't have (e.g. an Investor hitting
  `/admin/...`) — should be blocked, not silently render partial data.
- Uploading a document with an unexpected file type/size.

## 6. Automated test coverage that already exists

**Backend** (`cd offchain && ./mvnw test`):
`TenantContextTest`, `TenantIsolationIT` (cross-organization data isolation), `OfferingTest` /
`OfferingStatusTest` (state machine), `InvestmentTest`, `KycCaseTest`,
`ComplianceApplicationServiceTest`, `OffChainLifecycleIT` (full org → asset → offering → investor →
KYC → audit trail, end to end against a real MySQL test container).

**On-chain** (`cd onchain && forge test`):
Unit tests per contract (`RwaShiftTokenFactory`, `RwaShiftIdentityGateway`,
`RwaShiftDistributionRegistry`), an integration test for the full offering lifecycle, a fuzz test
for token issuance, and an invariant test for token accounting (`TokenAccounting.t.sol` +
`TokenAccountingHandler.sol`) that hammers the contracts with randomized sequences of operations
checking supply/balance invariants never break.

**Frontend**: type-checking only (`npx tsc --noEmit`) — there is currently no automated UI test
suite (no Playwright/Cypress/Jest component tests). Every frontend change in this project's history
has been manually verified via live browser testing. **This is the single biggest gap in test
coverage** — see [`critical-analysis.md`](critical-analysis.md).

## 7. Resetting to a clean state

```bash
docker compose down -v   # -v removes volumes: MySQL data, deployed contract addresses, the persisted JWT signing key
docker compose up --build
```

Without `-v`, data persists across restarts (MySQL volume, on-chain deployment address volume, and
the JWT signing key volume all survive a plain `down`/`up`) — useful for testing that sessions and
data really do survive a restart, which was a real bug fixed earlier in this platform's history
(the signing key used to regenerate on every boot, silently invalidating every session).
