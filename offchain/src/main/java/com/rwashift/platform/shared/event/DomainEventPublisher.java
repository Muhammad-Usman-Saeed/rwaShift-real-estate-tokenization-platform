package com.rwashift.platform.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin seam over Spring's event publisher so units depend on this interface rather than the
 * framework type directly, and so the transport can change later without touching units.
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    public DomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
