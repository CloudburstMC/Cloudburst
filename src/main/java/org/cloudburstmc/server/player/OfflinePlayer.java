package org.cloudburstmc.server.player;

import com.nukkitx.nbt.NbtMap;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.metadata.MetadataValue;

import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Represents a player that is currently offline.
 */
public class OfflinePlayer implements IPlayer {
    private final CloudServer server;
    private final NbtMap namedTag;

    /**
     * 初始化这个{@code OfflinePlayer}对象。<br>
     * Initializes the object {@code OfflinePlayer}.
     *
     * @param server 这个玩家所在服务器的{@code Server}对象。<br>
     *               The server this player is in, as a {@code Server} object.
     * @param uuid   这个玩家的UUID。<br>
     *               UUID of this player.
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public OfflinePlayer(CloudServer server, UUID uuid) {
        this(server, uuid, null);
    }

    public OfflinePlayer(CloudServer server, String name) {
        this(server, null, name);
    }

    public OfflinePlayer(CloudServer server, UUID uuid, String name) {
        this.server = server;

        NbtMap nbt;
        if (uuid != null) {
            nbt = this.server.getOfflinePlayerData(uuid, false);
        } else if (name != null) {
            nbt = this.server.getOfflinePlayerData(name, false);
        } else {
            throw new IllegalArgumentException("Name and UUID cannot both be null");
        }
        if (nbt == null) {
            nbt = NbtMap.EMPTY;
        }

        if (uuid != null) {
            nbt = nbt.toBuilder()
                    .putLong("UUIDMost", uuid.getMostSignificantBits())
                    .putLong("UUIDLeast", uuid.getLeastSignificantBits())
                    .build();
        } else {
            nbt = nbt.toBuilder().putString("NameTag", name).build();
        }
        this.namedTag = nbt;
    }

    @Override
    public boolean isOnline() {
        return this.getPlayer() != null;
    }

    @Override
    public String getName() {
        if (namedTag != null && namedTag.containsKey("NameTag")) {
            return namedTag.getString("NameTag");
        }
        return null;
    }

    @Override
    public UUID getServerId() {
        if (namedTag != null) {
            long least = namedTag.getLong("UUIDLeast");
            long most = namedTag.getLong("UUIDMost");

            if (least != 0 && most != 0) {
                return new UUID(most, least);
            }
        }
        return null;
    }

    public CloudServer getServer() {
        return server;
    }

    @Override
    public boolean isOp() {
        return this.server.isOp(this.getName().toLowerCase());
    }

    @Override
    public void setOp(boolean value) {
        if (value == this.isOp()) {
            return;
        }

        if (value) {
            this.server.addOp(this.getName().toLowerCase());
        } else {
            this.server.removeOp(this.getName().toLowerCase());
        }
    }

    @Override
    public boolean isBanned() {
        return this.server.getNameBans().isBanned(this.getName());
    }

    @Override
    public void setBanned(boolean value) {
        if (value) {
            this.server.getNameBans().addBan(this.getName(), null, null, null);
        } else {
            this.server.getNameBans().remove(this.getName());
        }
    }

    @Override
    public boolean isWhitelisted() {
        return this.server.isWhitelisted(this.getName().toLowerCase());
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            this.server.addWhitelist(this.getName().toLowerCase());
        } else {
            this.server.removeWhitelist(this.getName().toLowerCase());
        }
    }

    @Override
    public Player getPlayer() {
        return this.server.getPlayerExact(this.getName());
    }

    @Override
    public OptionalLong getFirstPlayed() {
        return this.namedTag != null ? OptionalLong.of(this.namedTag.getLong("firstPlayed")) : OptionalLong.empty();
    }

    @Override
    public OptionalLong getLastPlayed() {
        return this.namedTag != null ? OptionalLong.of(this.namedTag.getLong("lastPlayed")) : OptionalLong.empty();
    }

    @Override
    public boolean hasPlayedBefore() {
        return this.namedTag != NbtMap.EMPTY;
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.server.getPlayerMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return this.server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    public void removeMetadata(String metadataKey, PluginContainer owningPlugin) {
        this.server.getPlayerMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

}
