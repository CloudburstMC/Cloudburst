package org.cloudburstmc.server.player;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.CloudServer;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class CloudOfflinePlayer implements OfflinePlayer {

    @Getter
    private final CloudServer server;
    private final @Nullable String name;
    private @Nullable UUID uniqueId;

    public CloudOfflinePlayer(CloudServer server, @Nullable UUID uuid, @Nullable String name) {
        this.server = Objects.requireNonNull(server, "server");
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

        return this.name;
    }

    @Override
    public synchronized @Nullable UUID getUniqueId() {
        if (this.uniqueId == null && this.name != null) {
            CloudPlayer online = this.server.getPlayerExact(this.name);
            this.uniqueId = online != null ? online.getServerId() : this.server.lookupName(this.name).orElse(null);
        }
        return this.uniqueId;
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
        return uuid != null ? this.server.getPlayer(uuid) : Optional.empty();
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
        return uuid != null ? this.server.isWhitelisted(uuid) : this.server.isReservedWhitelisted(this.requireName());
    }

    @Override
    public void setWhitelisted(boolean value) {
        UUID uuid = this.getUniqueId();
        if (uuid != null) {
            this.server.setWhitelisted(uuid, this.getName(), value);
        } else {
            this.server.setReservedWhitelisted(this.requireName(), value);
        }
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
        return uuid != null ? this.server.isOperator(uuid) : this.server.isReservedOperator(this.requireName());
    }

    @Override
    public void setOp(boolean value) {
        UUID uuid = this.getUniqueId();
        if (uuid != null) {
            this.server.setOperator(uuid, this.getName(), value);
        } else {
            this.server.setReservedOperator(this.requireName(), value);
        }
    }

    private NbtMap data() {
        UUID uuid = this.getUniqueId();
        NbtMap data = uuid == null ? null : this.server.getOfflinePlayerData(uuid, false);
        return data != null ? data : NbtMap.EMPTY;
    }

    private String requireName() {
        return Objects.requireNonNull(this.name, "name");
    }

    private UUID requireUniqueId() {
        UUID uuid = this.getUniqueId();
        if (uuid == null) {
            throw new IllegalStateException("Player identity is unknown: " + this.getName());
        }

        return uuid;
    }
}
