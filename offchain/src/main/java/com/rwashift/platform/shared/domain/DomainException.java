package com.rwashift.platform.shared.domain;

/**
 * Base for business-rule violations raised inside a unit's domain/application layer (invalid
 * state transitions, invariant violations, etc). Distinct from validation errors (malformed
 * input, caught by Bean Validation before a service method even runs).
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
