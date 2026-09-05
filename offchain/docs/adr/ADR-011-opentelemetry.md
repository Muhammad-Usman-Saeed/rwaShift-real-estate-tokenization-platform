# ADR-011: OpenTelemetry for Observability

## Status

Accepted

## Context

A modular monolith with 16 capability units and an asynchronous blockchain-transaction subsystem
needs distributed-tracing-style visibility even within a single JVM: a request into `investment`
that triggers `compliance` eligibility, which triggers `tokenization` identity registration,
which completes asynchronously minutes later via the transaction poller, is hard to debug from
logs alone without a shared trace context.

## Decision

Use the **`opentelemetry-spring-boot-starter`** for automatic instrumentation (HTTP requests, JDBC
calls, scheduled tasks) and export traces/metrics via **OTLP over HTTP** to an `otel-collector`
sidecar service (`docker-compose.yml`, config at `otel/otel-collector-config.yaml`), reachable at
`:4318`. Every log line additionally carries a request-scoped correlation ID
(`CorrelationIdFilter`) and, once a span is active, `traceId`/`spanId`
(`logging.pattern.level` in `application.yml`), so logs and traces can be cross-referenced even
before a full tracing backend is wired to the collector.

## Consequences

- No manual span instrumentation was written in application code for V1 — auto-instrumentation
  covers HTTP/JDBC/scheduling, which covers the request → application-service → repository and
  `@Scheduled` poller paths that matter most for debugging the blockchain-transaction lifecycle.
- The collector is configured but does not itself ship to a specific backend (Jaeger/Tempo/etc.)
  in this repository — `otel-collector-config.yaml` is the integration point for whichever backend
  an actual deployment target uses; V1 ships the collector so the wiring is validated locally.
- `management.tracing.sampling.probability: 1.0` (100% sampling) is appropriate for V1's expected
  load; a production deployment should revisit this for cost/volume once traffic is non-trivial —
  not addressed by this ADR.
