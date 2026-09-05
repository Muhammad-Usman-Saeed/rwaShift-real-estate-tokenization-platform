package com.rwashift.platform.shared.event;

import java.time.Instant;

/**
 * Marker for events published across composite-unit boundaries. Units NEVER call another
 * unit's application services synchronously for cross-cutting side effects (e.g. Compliance
 * reacting to KYC verification) — they publish/subscribe to these instead, or depend on an
 * explicit {@code application/port} interface owned by the consuming unit and implemented by
 * an adapter in the producing unit's infrastructure layer.
 *
 * <p>V1 uses Spring's in-process {@link org.springframework.context.ApplicationEventPublisher}
 * (synchronous, same-transaction by default; {@code @TransactionalEventListener} is used where
 * listeners must run only after commit). Because every event here is a plain, serializable
 * record, swapping to an actual broker (e.g. outbox + Kafka) later touches only
 * {@link DomainEventPublisher}'s implementation, not call sites.
 */
public interface DomainEvent {

    String eventId();

    Instant occurredAt();

    String organizationId();
}
