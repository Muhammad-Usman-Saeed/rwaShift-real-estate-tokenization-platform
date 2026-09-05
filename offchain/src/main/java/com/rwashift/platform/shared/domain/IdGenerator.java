package com.rwashift.platform.shared.domain;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * Application-level identifiers are ULIDs (lexicographically sortable, time-ordered), never
 * database auto-increment integers and never blockchain addresses/tx hashes. Blockchain
 * identifiers are correlation data on an entity, not its primary key (see ADR-007).
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String newId() {
        return UlidCreator.getUlid().toString();
    }
}
