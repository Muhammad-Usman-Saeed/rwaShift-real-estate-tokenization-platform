# ADR-009: Spring Authorization Server for OAuth2/OIDC

## Status

Accepted

## Context

The platform needs standards-based authentication/authorization (OAuth2/OIDC) for its own web
client, with tenant (`organizationId`) and role information available on every authenticated
request without a lookup on every call. Options considered: an external IdP (Keycloak, Auth0) vs.
Spring's own Authorization Server module.

## Decision

Run **Spring Authorization Server** in-process, as part of the same deployable. Two
`SecurityFilterChain`s, ordered explicitly:

- `@Order(1)`: the Authorization Server's own endpoints (`/oauth2/authorize`, `/oauth2/token`,
  `/.well-known/openid-configuration`, etc.).
- `@Order(2)`: the resource server for `/api/**` (JWT bearer validation against the AS's own
  issuer) plus form login for the AS's login page.

`JdbcRegisteredClientRepository`/`JdbcOAuth2AuthorizationService`/
`JdbcOAuth2AuthorizationConsentService` persist client registrations and authorization state in
the same MySQL database. `OAuth2ClientBootstrap` idempotently registers the platform's own
`rwashift-web` client on every boot (safe to run repeatedly — checks for existing registration
first). `TokenClaimsCustomizer` adds `org_id`, `roles`, and `investor_id` as custom JWT claims,
read back into `TenantContext` per request without an extra database round-trip.

## Consequences

- No external IdP dependency for V1 — one fewer operational system, at the cost of Spring
  Authorization Server being a comparatively newer/less battle-tested project than Keycloak.
- RSA keypair for token signing is generated at startup unless explicitly configured — acceptable
  for local/demo use; a production deployment must configure a persistent, properly-managed key
  (not covered by this ADR, since production key management is a deployment-environment concern).
- Every request's `TenantContext` (`userId, organizationId, roles, investorId`) comes directly off
  the validated JWT's custom claims — tenant isolation checks (`requireSameOrganization()`, see
  the multi-tenancy discussion in the top-level README) never require an extra database lookup to
  determine "which org does this caller belong to."
