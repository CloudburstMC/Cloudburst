package org.cloudburstmc.server.player;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserWhitelistEntry(@Nullable UUID uuid, @Nullable String name) implements StoredUserEntry {

    public UserWhitelistEntry {
        if (uuid == null && name == null) {
            throw new IllegalArgumentException("Entry needs a UUID or a name");
        }

        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
    }
}
