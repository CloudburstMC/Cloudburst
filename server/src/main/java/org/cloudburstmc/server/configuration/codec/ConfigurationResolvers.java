package org.cloudburstmc.server.configuration.codec;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Runtime lookups used by codecs, independent of YAML parsing and file storage.
 */
public record ConfigurationResolvers(
        Function<String, ? extends Level> levels,
        Function<Identifier, BlockState> blocks,
        Function<UUID, ? extends OfflinePlayer> playerIds,
        Function<String, ? extends OfflinePlayer> playerNames) {

    public ConfigurationResolvers {
        Objects.requireNonNull(levels, "levels");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(playerNames, "playerNames");
    }

    public static ConfigurationResolvers unavailable() {
        return new ConfigurationResolvers(id -> null, id -> null, id -> null, name -> null);
    }
}
