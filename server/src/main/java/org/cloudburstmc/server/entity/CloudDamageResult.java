package org.cloudburstmc.server.entity;

/**
 * Keeps listener cancellation separate from immunity, blocking and hurt-cooldown rejection.
 */
public record CloudDamageResult(boolean applied, boolean cancelled) {
}
