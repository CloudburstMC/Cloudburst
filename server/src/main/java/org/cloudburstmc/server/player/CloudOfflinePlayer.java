package org.cloudburstmc.server.player;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.CloudServer;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class CloudOfflinePlayer implements OfflinePlayer {
    @Getter
    private final CloudServer server;
    private final @Nullable UUID uniqueId;
    private final @Nullable String name;
    private final NbtMap namedTag;

    public CloudOfflinePlayer(Server server, UUID uuid) {
        this(server, uuid, null);
    }

    public CloudOfflinePlayer(Server server, String name) {
        this(server, null, name);
    }

    public CloudOfflinePlayer(Server server, @Nullable UUID uuid, @Nullable String name) {
        this.server = (CloudServer) server;
        this.uniqueId = uuid;

        NbtMap loadedTag;
        if (uuid != null) {
            loadedTag = this.server.getOfflinePlayerData(uuid, false);
        } else if (name != null) {
            loadedTag = this.server.getOfflinePlayerData(name, false);
        } else {
            throw new IllegalArgumentException("Name and UUID cannot both be null");
        }

        NbtMap tag = loadedTag == null ? NbtMap.EMPTY : loadedTag;
        if (uuid != null) {
            tag = tag.toBuilder()
                    .putLong("UUIDMost", uuid.getMostSignificantBits())
                    .putLong("UUIDLeast", uuid.getLeastSignificantBits())
                    .build();
        }

        if (name != null && !tag.containsKey("NameTag")) {
            tag = tag.toBuilder().putString("NameTag", name).build();
        }

        this.namedTag = tag;
        this.name = tag.containsKey("NameTag") ? tag.getString("NameTag") : name;
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
    public @Nullable UUID getUniqueId() {
        if (this.uniqueId != null) {
            return this.uniqueId;
        }

        if (!this.namedTag.containsKey("UUIDMost") || !this.namedTag.containsKey("UUIDLeast")) {
            return null;
        }

        long most = this.namedTag.getLong("UUIDMost");
        long least = this.namedTag.getLong("UUIDLeast");
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
        String name = this.requireName();
        return this.server.getNameBans().isBanned(name);
    }

    @Override
    public void setBanned(boolean value) {
        String name = this.requireName();
        if (value) {
            this.server.getNameBans().addBan(name, null, null, null);
        } else {
            this.server.getNameBans().remove(name);
        }
    }

    @Override
    public boolean isWhitelisted() {
        return this.server.isWhitelisted(this.requireName());
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            this.server.addWhitelist(this.requireName());
        } else {
            this.server.removeWhitelist(this.requireName());
        }
    }

    @Override
    public OptionalLong getFirstPlayed() {
        return this.namedTag.containsKey("firstPlayed")
                ? OptionalLong.of(this.namedTag.getLong("firstPlayed"))
                : OptionalLong.empty();
    }

    @Override
    public OptionalLong getLastPlayed() {
        return this.namedTag.containsKey("lastPlayed")
                ? OptionalLong.of(this.namedTag.getLong("lastPlayed"))
                : OptionalLong.empty();
    }

    @Override
    public boolean hasPlayedBefore() {
        return this.namedTag != NbtMap.EMPTY;
    }

    @Override
    public boolean isOp() {
        return this.server.isOp(this.requireName());
    }

    @Override
    public void setOp(boolean value) {
        String name = this.requireName();
        if (value == this.server.isOp(name)) {
            return;
        }

        if (value) {
            this.server.addOp(name);
        } else {
            this.server.removeOp(name);
        }
    }

    private String requireName() {
        String name = this.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Offline player has no known name");
        }

        return name;
    }
}
