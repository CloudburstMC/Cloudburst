package org.cloudburstmc.server.player;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerOpListEntry(@Nullable UUID uuid, @Nullable String name, int level, boolean bypassesPlayerLimit) implements StoredUserEntry {

    public ServerOpListEntry {
        if (uuid == null && name == null) {
            throw new IllegalArgumentException("Entry needs a UUID or a name");
        }

        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }

        if (level < 0 || level > 4) {
            throw new IllegalArgumentException("Operator level must be between 0 and 4");
        }
    }
}
