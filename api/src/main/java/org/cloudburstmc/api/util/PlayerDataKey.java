package org.cloudburstmc.api.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies stored player data by UUID. A display name never substitutes for identity.
 *
 * @param uniqueId the player's UUID
 * @param name the last known name, or null when unknown
 */
public record PlayerDataKey(UUID uniqueId, @Nullable String name) {
    public PlayerDataKey {
        Objects.requireNonNull(uniqueId, "uniqueId");
    }

    /**
     * Creates a key when no name is known.
     */
    public static PlayerDataKey of(UUID uniqueId) {
        return new PlayerDataKey(uniqueId, null);
    }

    /**
     * Creates a UUID key with optional display metadata.
     */
    public static PlayerDataKey of(UUID uniqueId, @Nullable String name) {
        return new PlayerDataKey(uniqueId, name);
    }

    /**
     * Returns the UUID as a stable string identifier.
     */
    public String getStorageId() {
        return uniqueId.toString();
    }
}
