package org.cloudburstmc.server.entity;

/**
 * Describes how a target resolved an incoming hit.
 */
public enum CloudDamageResult {
    /**
     * The hit used the normal combat path.
     */
    APPLIED,
    /**
     * The target accepted the hit and owns its feedback and effects.
     */
    HANDLED,
    /**
     * The target did not accept the hit.
     */
    REJECTED,
    /**
     * An event listener cancelled the hit or its outcome.
     */
    CANCELLED;

    /**
     * Returns whether the target accepted the hit.
     */
    public boolean applied() {
        return this == APPLIED || this == HANDLED;
    }

    public boolean cancelled() {
        return this == CANCELLED;
    }
}
