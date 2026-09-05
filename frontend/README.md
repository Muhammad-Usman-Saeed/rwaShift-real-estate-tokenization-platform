# RWA Shift Real Estate — Frontend

Institutional Next.js frontend for **RWA Shift Real Estate**: structure, issue, and manage
tokenized real estate investments. One application, three role-based experiences — **Issuer
Portal**, **Investor Portal**, **Platform Admin** — sharing authentication, layout, design system,
API client, wallet infrastructure, and error handling, per the product's architecture mandate.

This is institutional real-estate investment software, not a crypto trading dApp. See
[Design system](#design-system) for how that shows up in the UI.

## Stack

- **Next.js 14** (App Router), **TypeScript** (strict), **React 18**
- **TanStack Query** for all server state — no `fetch` calls in components, see [API layer](#api-layer)
- **React Hook Form + Zod** for forms
- **wagmi + viem** for the minimal wallet integration
- **Auth.js (NextAuth v5)** — OIDC client against the platform's own Spring Authorization Server
- Tailwind CSS + a small hand-built component system (`components/ui`) — no third-party UI kit
- **Vitest + Testing Library** for tests
- **recharts** for the reporting charts

## Getting started

```bash
cp .env.example .env.local   # fill in AUTH_SECRET at minimum
npm install
npm run dev
```

Requires the [`offchain/`](../offchain) Spring Boot API running (`AUTH_ISSUER`, `API_BASE_URL` in
`.env.local` point at it — defaults assume `http://localhost:9000` / `http://localhost:8080/api/v1`,
matching that project's own local-dev defaults).

```bash
npm run typecheck   # tsc --noEmit
npm run lint
npm test            # vitest run
npm run build        # production build
```

## Architecture

### One app, three portals

```
src/app/
├── issuer/     — route group, protected by middleware + ISSUER_* / COMPLIANCE_OFFICER / PLATFORM_ADMIN roles
├── investor/   — route group, protected by INVESTOR / PLATFORM_ADMIN roles
├── admin/      — route group, protected by PLATFORM_ADMIN / COMPLIANCE_OFFICER roles
├── login/, unauthorized/, api/auth/[...nextauth]/
```

Each portal has its own `layout.tsx` (`PortalShell` + role-specific nav from
`components/layout/nav-config.tsx`), but all three share:

- `providers/AppProviders.tsx` — Auth.js `SessionProvider` → TanStack `QueryProvider` →
  `WalletProvider` (wagmi) → `ToastProvider`.
- `lib/api/*` — the typed API client (below).
- `components/ui/*` — the design system.
- `components/domain/*` — cross-portal business components (`StatusPill`, `OwnershipFlowDiagram`,
  `BlockchainTxDetails`, `DocumentsPanel`, `ConnectWalletButton`, …).

A user with more than one portal's access (a `COMPLIANCE_OFFICER` has both issuer-org and
platform-admin-style capabilities per the backend's own `@PreAuthorize` rules) gets a portal
switcher in the top bar — see `components/layout/Topbar.tsx`.

### Authentication

**Platform login is the only authentication.** Wallet connection is a separate, secondary step
that never substitutes for it (`lib/auth/session.ts` vs. `lib/wallet/useLinkedWallet.ts` are
deliberately independent).

```
Platform Login (OIDC, Auth.js)
    ↓
Authenticated session — roles / org_id / investor_id read from the ACCESS TOKEN's custom claims
    ↓
Portal (issuer / investor / admin), gated by middleware.ts
```

`lib/auth/auth.ts` runs a generic OIDC client against the backend's Spring Authorization Server
(`AUTH_ISSUER`). One subtlety: the backend's `TokenClaimsCustomizer` bakes `roles`/`org_id`/
`investor_id` into the **access token** only, never the ID token — so the `jwt` callback decodes
the access token directly (`lib/auth/jwt.ts`) rather than relying on Auth.js's own `profile()`
parsing (which reads the ID token). Refresh-token rotation is implemented against the
15-minute-TTL access tokens the backend issues.

**`middleware.ts` route protection is UX only** — it just keeps a signed-in user from seeing a
portal their role doesn't cover, and bounces unauthenticated visitors to `/login`. Every API call
is independently authorized by the backend (`TenantContext` + `@PreAuthorize`), which remains
authoritative. `components/layout/RoleGate.tsx` does the same UX-only gating for individual
actions (e.g. hiding an Approve button).

### API layer

`lib/api/` — one file per backend capability (`assets.ts`, `offerings.ts`, `investments.ts`,
`tokenization.ts`, `ownership.ts`, `distributions.ts`, `kyc.ts`, `compliance.ts`, `audit.ts`, …),
each exporting plain typed functions that call through `lib/api/client.ts`'s `apiRequest`. That
client:

- attaches `Authorization: Bearer <token>` and a fresh `X-Correlation-Id` per request (propagated
  to the backend's own correlation/trace machinery — see `shared.web.CorrelationIdFilter` in
  `offchain/`);
- parses RFC 9457 Problem Details on any non-2xx response and throws a typed `ApiError`
  (`lib/errors/problem-details.ts`) rather than a raw `Response`.

Two hooks (`lib/hooks/useAuthedQuery.ts`, `useAuthedMutation.ts`) wrap TanStack Query so every
screen's data-fetching threads the session's access token automatically and holds queries off
until a token exists — components call these, never `fetch`, never the raw `apiRequest` directly.
`lib/api/query-keys.ts` is the single source of truth for query keys, so cache invalidation after
a mutation can't drift between files.

### Error handling

`lib/errors/problem-details.ts` maps the backend's Problem Details `type`/`detail` into a stable,
user-safe `{ title, message, fieldErrors?, correlationId? }` — with specific overrides for the
workflow failures the spec calls out (KYC required, ineligible, payment issue, blockchain revert,
RPC/connection issue) layered on top of the generic per-status-code fallback. `ErrorBanner`
(`components/ui/ErrorBanner.tsx`) is the one place this ever gets rendered. Raw stack traces or
backend internals are never shown; unauthorized (401) never echoes the backend's own detail text.

### Wallet integration

`lib/wallet/wagmi-config.ts` — a single injected-wallet connector (MetaMask or equivalent),
Sepolia + a locally-defined Anvil chain (31337). Deliberately minimal, per the product spec: no
WalletConnect modal, no multi-wallet chooser for V1.

Two real on-chain interactions, both read/write directly against the T-REX `Token` contract (no
backend involvement — matches `onchain/README.md`'s own documented "any holder" transfer
semantics):

- `lib/wallet/useTokenBalance.ts` — live `balanceOf`/`symbol` reads, shown in Portfolio's
  On-Chain Holdings section next to (never merged with) the application's own investment records.
- The **Controlled Transfer Demo** (`/investor/transfer-demo`) — a genuine `eth_call` simulation
  of `Token.transfer` before ever submitting it, so a rejected transfer is a real T-REX revert
  (`identityRegistry.isVerified` + compliance module checks inside `_transfer`), never a scripted
  UI state. See that page's comments for exactly what's simulated vs. submitted.

No wallet signature is ever requested for anything the backend can't independently verify — see
the comment in `lib/wallet/useLinkedWallet.ts` for why wallet linking has no signature step in V1.

### Design system

`components/ui/*` — Tailwind tokens (`tailwind.config.ts`) chosen for an institutional, financial
feel: a navy/ink primary palette, a restrained gold accent used only for emphasis (tokenization
progress, key figures), semantic status colors that are never the only signal (every `StatusPill`
carries a text label, never color alone — see the accessibility notes below).

Explicitly avoided, per the product brief: neon crypto palettes, token price tickers, candlestick
charts, APY-style DeFi framing, NFT visuals.

### Accessibility

- Every interactive control is reachable and operable by keyboard; focus states use a visible
  ring (`globals.css`'s `:focus-visible` rule), not just color.
- Status is always conveyed by a text label (see `StatusPill`), never by color or icon alone.
- Forms use associated `<label>`s, `aria-invalid`, and `role="alert"` error text
  (`components/ui/Field.tsx`).
- Semantic HTML throughout (`<table>`, `<nav aria-label>`, `<dl>` for term/value pairs).

## Real backend integration — no static mocks

Every screen calls the actual `offchain/` Spring Boot API. While wiring the admin portal and the
investor-facing screens to real endpoints, several backend gaps were found and fixed (not routed
around) — see `offchain`'s own git history for the specifics, but in summary: several list
endpoints were hard-scoped to the caller's own organization, which breaks for `PLATFORM_ADMIN`
(no organization of their own) and for `INVESTOR` (never has an organization — investors browsing
the cross-org marketplace of open offerings, or admins reviewing every organization's submissions,
needed those endpoints to actually return something). A `GET /investors/me/wallet`-equivalent
(`POST /investors/me/wallet`) was added so wallet linking is a genuine standalone step, matching
the product spec's flow rather than being bundled permanently into onboarding. Blockchain
transaction and cross-offering deployment listing endpoints were added for the admin portal, since
none existed before this frontend needed them.

**Demo payment confirmation** (`Investment.confirmPayment`) is intentionally `PLATFORM_ADMIN`-only
on the backend — it simulates a bank webhook a real payment provider would call, not something an
investor self-serves. The **Admin → Compliance** screen has a "Confirm Payment (Demo)" queue for
exactly this — you'll need to click it there to walk the investment flow to completion in a live
demo, matching how the backend's own authorization was designed.

## Environment variables

See `.env.example`. The essentials:

| Variable | Purpose |
|---|---|
| `AUTH_URL`, `AUTH_SECRET` | Auth.js base URL + session encryption secret |
| `AUTH_ISSUER`, `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET` | OIDC client against the backend's Spring Authorization Server — must match `OAuth2ClientBootstrap` in `offchain/` |
| `API_BASE_URL` / `NEXT_PUBLIC_API_BASE_URL` | Backend REST API base (server-side / browser — same value unless you're behind a different Docker-network hostname) |
| `NEXT_PUBLIC_CHAIN` | `sepolia` or `anvil` — drives explorer links and the default chain label |
| `NEXT_PUBLIC_SEPOLIA_RPC_URL`, `NEXT_PUBLIC_ANVIL_RPC_URL` | RPC endpoints wagmi reads from directly |

## Running the full demo scenario

With `offchain/` and `onchain/` running per their own READMEs, and this app's `.env.local` pointed
at them:

1. Sign in as an issuer admin → **Issuer → Assets → Create Asset** (Dubai Business Tower, $10M).
2. Asset detail → **Legal Structure** tab → create the SPV.
3. **Issuer → Offerings → Create Offering** — the flagship guided form shows the
   $10M → $2M/20% → 20,000 units → $100/unit derivation live.
4. Submit for review → sign in as `PLATFORM_ADMIN` → **Admin → Offering Approvals** → Approve.
5. Back as issuer → offering detail → **Tokenize Offering** — real backend-driven deployment
   progress, no fake confirmation.
6. Sign in as an investor → **Complete Profile & Link Wallet** → **KYC → Submit Verification** →
   sign in as admin/compliance → **Admin → KYC Reviews** → Verify.
7. Investor → **Opportunities** → the now-open offering → **Invest** → $10,000 → 100 units →
   agree to terms → Confirm.
8. Admin → **Compliance** → **Confirm Payment (Demo)** for that investment.
9. Investment flow auto-polls through token issuance → **Investment Complete** → **Portfolio**.
10. **Issuer → Offerings → [offering] → Ownership Register** shows the new investor.
11. Investor → **Portfolio → Try a Compliance-Controlled Transfer** to an unverified wallet →
    **TRANSFER REJECTED** (real on-chain revert) → verify that investor via KYC → retry →
    **TRANSFER CONFIRMED**.
12. Issuer → **Distributions → Create Distribution** → Calculate Entitlements → Approve.
13. Investor → **Distributions** shows the entitlement; Issuer → **Reports** shows the portfolio
    charts; Admin → **Audit Logs** shows the full trail end to end.
