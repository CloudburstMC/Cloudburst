package org.cloudburstmc.server.configuration.codec;

import org.cloudburstmc.api.configuration.ConfigurationCodec;
import org.cloudburstmc.api.player.OfflinePlayer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persists a player identity, not a snapshot of player state.
 */
public class OfflinePlayerConfigurationCodec implements ConfigurationCodec<OfflinePlayer> {
    private final ConfigurationResolvers resolvers;

    public OfflinePlayerConfigurationCodec(ConfigurationResolvers resolvers) {
        this.resolvers = Objects.requireNonNull(resolvers, "resolvers");
    }

    @Override
    public Map<String, Object> encode(OfflinePlayer player) {
        UUID id = player.getUniqueId();
        if (id != null) {
            return Map.of("uuid", id.toString());
        }

        String name = player.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player reference requires a UUID or name");
        }

        return Map.of("name", name);
    }

    @Override
    public OfflinePlayer decode(Map<String, Object> data) {
        if (data.containsKey("uuid") == data.containsKey("name")) {
            throw new IllegalArgumentException("Player reference requires exactly one UUID or name");
        }

        OfflinePlayer player;

        if (data.get("uuid") instanceof String id) {
            player = resolvers.playerIds().apply(UUID.fromString(id));
        } else if (data.get("name") instanceof String name && !name.isBlank()) {
            player = resolvers.playerNames().apply(name);
        } else {
            throw new IllegalArgumentException("Player reference requires a UUID or name");
        }

        if (player == null) {
            throw new IllegalArgumentException("Player resolution is unavailable");
        }

        return player;
    }
}
