package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;

public record ServerOpListEntry(UUID uuid, @Nullable String name, boolean bypassesPlayerLimit) implements StoredUserEntry {

    public ServerOpListEntry {
        Objects.requireNonNull(uuid, "uuid");
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
    }
}
