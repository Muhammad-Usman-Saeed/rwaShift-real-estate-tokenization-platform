# RWA Shift Real Estate

Structure, issue, and manage tokenized real estate investments. An institutional real-estate
tokenization platform — not a crypto trading dApp.

```
onchain/    ERC-3643 (T-REX) smart contracts — Foundry           see onchain/README.md
offchain/   REST API + OAuth2/OIDC Authorization Server — Spring Boot   see offchain/README.md
frontend/   Issuer / Investor / Platform Admin portals — Next.js        see frontend/README.md
```

Each project has its own README with architecture detail, ADRs, and how to run it standalone.
This file covers running **all three together**.

## Run the whole stack

```bash
cp .env.example .env   # optional — every value has a working local default
docker compose up --build
```

This single command:

1. Starts MySQL and an Anvil dev chain.
2. Deploys the full on-chain platform (`contracts-deploy`, a one-shot service) — the ERC-3643/
   T-REX suite plus RWA Shift's own `RwaShiftTokenFactory`/`RwaShiftIdentityGateway`/
   `RwaShiftDistributionRegistry` — to that Anvil instance, and writes the resulting contract
   addresses to a shared volume.
3. Starts the offchain app, whose entrypoint (`offchain/docker/entrypoint.sh`) reads those
   addresses before launching, so `TOKEN_FACTORY_ADDRESS` etc. are wired automatically — no
   manual copy-paste from `deployments/local.json`, unlike running the pieces individually.
4. Starts the frontend, pointed at the offchain app.
5. Seeds the canonical demo scenario (ABC Real Estate / Dubai Business Tower) — set
   `RWASHIFT_SEED_ENABLED=false` in `.env` to skip.

Once it's up (the offchain app and frontend take the longest — Maven and `next build` both run
inside their respective image builds):

- **App**: [http://localhost:3000](http://localhost:3000)
- **API**: [http://localhost:8080](http://localhost:8080) (Swagger UI at `/swagger-ui.html`)
- **Anvil RPC**: `http://localhost:8545`

See [`frontend/README.md`](frontend/README.md#running-the-full-demo-scenario) for the full
guided walkthrough (create asset → legal structure → offering → approve → tokenize → verify
investor → invest → distribute → audit).

### Why a custom entrypoint for the offchain service

The on-chain contract addresses the offchain app needs (`TOKEN_FACTORY_ADDRESS`,
`IDENTITY_GATEWAY_ADDRESS`, `DISTRIBUTION_REGISTRY_ADDRESS`) don't exist until `contracts-deploy`
actually runs `DeployLocal.s.sol` against Anvil — they can't be hardcoded as compose
`environment:` values. `offchain/docker/entrypoint.sh` reads them from the deployment JSON
`contracts-deploy` writes to a shared volume, exports them, then starts the app.

### Why the frontend has two issuer/API URLs

The offchain app serves both the REST API and the OAuth2 Authorization Server on the same origin.
Inside Docker, that origin means two different things depending on who's asking:

- The user's **browser** needs `http://localhost:8080` (the published port) — for the
  `/oauth2/authorize` redirect and every direct API call the client makes.
- The frontend's own **Node server**, running inside the `frontend` container, needs
  `http://app:8080` (the Docker network service name) for everything it does server-side:
  discovery, token exchange, `/userinfo`, JWKS, and refresh-token calls — from the frontend
  container's point of view, `localhost:8080` would mean itself, not the backend.

`AUTH_ISSUER`/`NEXT_PUBLIC_API_BASE_URL` carry the browser-facing address; `AUTH_INTERNAL_ISSUER`/
`API_BASE_URL` carry the container-to-container one. Running the frontend outside Docker (`npm run
dev`) doesn't need this split — see `frontend/.env.example`.

## Run everything except Docker (local development)

Each project's own README documents running it directly on the host — useful for fast iteration
on one piece without rebuilding images:

```bash
# terminal 1 — on-chain
cd onchain && anvil
cd onchain && forge script script/DeployLocal.s.sol:DeployLocal --rpc-url http://127.0.0.1:8545 --broadcast

# terminal 2 — off-chain (copy the addresses from onchain/deployments/local.json into offchain/.env or your shell)
cd offchain && ./mvnw spring-boot:run

# terminal 3 — frontend
cd frontend && npm run dev
```

## Repository layout

| Path | Stack | Purpose |
|---|---|---|
| [`onchain/`](onchain/README.md) | Foundry / Solidity | ERC-3643 token issuance, transfer-eligibility enforcement, distribution anchoring |
| [`offchain/`](offchain/README.md) | Java 21 / Spring Boot / MySQL | Everything that isn't the token contracts: organizations, assets, offerings, KYC, compliance, investments, blockchain orchestration, reporting, audit |
| [`frontend/`](frontend/README.md) | Next.js / TypeScript | Issuer Portal, Investor Portal, Platform Admin — one app, three role-based experiences |
