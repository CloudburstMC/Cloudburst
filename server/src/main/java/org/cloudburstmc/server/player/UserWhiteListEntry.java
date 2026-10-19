package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;

public record UserWhiteListEntry(UUID uuid, @Nullable String name) implements StoredUserEntry {

    public UserWhiteListEntry {
        Objects.requireNonNull(uuid, "uuid");
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
    }
}
