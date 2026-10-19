package org.cloudburstmc.server.player;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.CloudServer;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class CloudOfflinePlayer implements OfflinePlayer {

    @Getter
    private final CloudServer server;
    private final @Nullable UUID uniqueId;
    private final @Nullable String name;
    private @Nullable NbtMap namedTag;

    public CloudOfflinePlayer(Server server, UUID uuid) {
        this(server, uuid, null);
    }

    public CloudOfflinePlayer(Server server, String name) {
        this(server, null, name);
    }

    public CloudOfflinePlayer(Server server, @Nullable UUID uuid, @Nullable String name) {
        this.server = (CloudServer) server;
        this.uniqueId = uuid;
        this.name = name;

        if (uuid == null && name == null) {
            throw new IllegalArgumentException("Name and UUID cannot both be null");
        }
    }

    @Override
    public @Nullable String getName() {
        Optional<Player> player = this.getPlayer();
        if (player.isPresent()) {
            return player.get().getName();
        }

        if (this.name != null) {
            return this.name;
        }

        NbtMap data = this.data();
        return data.containsKey("NameTag") ? data.getString("NameTag") : null;
    }

    @Override
    public @Nullable UUID getUniqueId() {
        if (this.uniqueId != null) {
            return this.uniqueId;
        }

        if (this.name != null) {
            CloudPlayer online = this.server.getPlayerExact(this.name);
            if (online != null) {
                return online.getServerId();
            }

            Optional<UUID> known = this.server.lookupName(this.name);
            if (known.isPresent()) {
                return known.get();
            }
        }

        NbtMap data = this.data();
        if (!data.containsKey("UUIDMost") || !data.containsKey("UUIDLeast")) {
            return null;
        }

        long most = data.getLong("UUIDMost");
        long least = data.getLong("UUIDLeast");
        return most != 0 || least != 0 ? new UUID(most, least) : null;
    }

    @Override
    public boolean isOnline() {
        return this.getPlayer().isPresent();
    }

    @Override
    public boolean isConnected() {
        return this.getPlayer().map(Player::isConnected).orElse(false);
    }

    @Override
    public Optional<Player> getPlayer() {
        UUID uuid = this.getUniqueId();
        if (uuid != null) {
            return this.server.getPlayer(uuid);
        }

        return Optional.ofNullable(this.name).map(this.server::getPlayerExact);
    }

    @Override
    public boolean isBanned() {
        UUID uuid = this.getUniqueId();
        return uuid != null && this.server.getPlayerBanList().isBanned(uuid);
    }

    @Override
    public BanEntry<UUID> ban(@Nullable String reason, @Nullable Instant expiration, @Nullable String source) {
        return this.server.getPlayerBanList().addBan(this.requireUniqueId(), reason, expiration, source);
    }

    @Override
    public boolean isWhitelisted() {
        UUID uuid = this.getUniqueId();
        return uuid != null && this.server.isWhitelisted(uuid);
    }

    @Override
    public void setWhitelisted(boolean value) {
        this.server.setWhitelisted(this.requireUniqueId(), this.getName(), value);
    }

    @Override
    public OptionalLong getFirstPlayed() {
        NbtMap data = this.data();
        return data.containsKey("firstPlayed")
                ? OptionalLong.of(data.getLong("firstPlayed"))
                : OptionalLong.empty();
    }

    @Override
    public OptionalLong getLastPlayed() {
        NbtMap data = this.data();
        return data.containsKey("lastPlayed")
                ? OptionalLong.of(data.getLong("lastPlayed"))
                : OptionalLong.empty();
    }

    @Override
    public boolean hasPlayedBefore() {
        return !this.data().isEmpty();
    }

    @Override
    public boolean isOp() {
        UUID uuid = this.getUniqueId();
        return uuid != null && this.server.isOperator(uuid);
    }

    @Override
    public void setOp(boolean value) {
        this.server.setOperator(this.requireUniqueId(), this.getName(), value);
    }

    private synchronized NbtMap data() {
        if (this.namedTag == null) {
            NbtMap loaded = this.uniqueId != null
                    ? this.server.getOfflinePlayerData(this.uniqueId, false)
                    : this.server.getOfflinePlayerData(this.name, false);
            this.namedTag = loaded == null ? NbtMap.EMPTY : loaded;
        }

        return this.namedTag;
    }

    private UUID requireUniqueId() {
        UUID uuid = this.getUniqueId();
        if (uuid == null) {
            throw new IllegalStateException("Player identity is unknown: " + this.getName());
        }

        return uuid;
    }
}
