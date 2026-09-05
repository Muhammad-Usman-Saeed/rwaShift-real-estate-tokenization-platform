package com.rwashift.platform.shared.domain;

/**
 * Raised by any aggregate's state machine (Offering, KYC, Investment, Distribution, ...) when
 * an explicit transition command is invoked from a state that does not permit it. State is
 * never mutated by directly assigning a status field from outside the aggregate.
 */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String aggregateType, String currentState, String attemptedTransition) {
        super("%s in state '%s' does not allow transition '%s'".formatted(aggregateType, currentState, attemptedTransition));
    }
}
