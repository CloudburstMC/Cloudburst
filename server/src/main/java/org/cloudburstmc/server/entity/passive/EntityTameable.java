package org.cloudburstmc.server.entity.passive;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Tameable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.server.CloudServer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.OWNER_EID;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.SITTING;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.TAMED;

public abstract class EntityTameable extends Animal implements Tameable {

    private static final String OWNER_UUID = "Cloudburst.OwnerUUID";
    private static final String TAMED_STATE = "Cloudburst.Tamed";
    private static final String SITTING_STATE = "Sitting";

    private @Nullable UUID ownerUniqueId;

    protected EntityTameable(EntityType<? extends Tameable> type, Location location) {
        super(type, location);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        this.ownerUniqueId = readOwnerUniqueId(tag);
        this.data.setFlag(TAMED, this.ownerUniqueId != null || tag.getBoolean(TAMED_STATE));
        this.data.setFlag(SITTING, tag.getBoolean(SITTING_STATE));
        this.updateOwnerMetadata();
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        if (this.ownerUniqueId != null) {
            tag.putString(OWNER_UUID, this.ownerUniqueId.toString());
        } else if (this.isTamed()) {
            tag.putBoolean(TAMED_STATE, true);
        }
        tag.putBoolean(SITTING_STATE, this.isSitting());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.addAll(Set.of(OWNER_UUID, TAMED_STATE, SITTING_STATE));
        return Set.copyOf(keys);
    }

    @Override
    public @Nullable UUID getOwnerUniqueId() {
        return this.ownerUniqueId;
    }

    @Override
    public void setOwnerUniqueId(@Nullable UUID ownerUniqueId) {
        this.ownerUniqueId = ownerUniqueId;
        this.data.setFlag(TAMED, ownerUniqueId != null);
        if (ownerUniqueId != null) {
            this.setTarget(null);
        }
        this.updateOwnerMetadata();
        this.markChunkDirty();
    }

    @Override
    public @Nullable Player getOwner() {
        CloudServer server = this.getServer();
        UUID ownerUniqueId = this.ownerUniqueId;
        if (ownerUniqueId == null || server == null) {
            return null;
        }

        return server.getPlayer(ownerUniqueId).filter(player -> !player.isClosed()).orElse(null);
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        switch (owner) {
            case null -> this.setOwnerUniqueId(null);
            case Player player -> this.setOwnerUniqueId(player.getServerId());
            default -> throw new IllegalArgumentException("Only players can own tameable mobs");
        }
    }

    @Override
    public boolean isTamed() {
        return this.data.getFlag(TAMED);
    }

    @Override
    public void setTamed(boolean tamed) {
        if (tamed) {
            this.data.setFlag(TAMED, true);
            this.markChunkDirty();
        } else {
            this.setOwnerUniqueId(null);
        }
    }

    @Override
    public boolean isSitting() {
        return this.data.getFlag(SITTING);
    }

    @Override
    public void setSitting(boolean sitting) {
        this.data.setFlag(SITTING, sitting);
        this.markChunkDirty();
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        this.updateOwnerMetadata();
        return super.tickEntity(currentTick, elapsedTicks);
    }

    @Override
    protected BedrockPacket createAddEntityPacket() {
        this.updateOwnerMetadata();
        return super.createAddEntityPacket();
    }

    private void updateOwnerMetadata() {
        Player owner = this.getOwner();
        long actorId = owner != null && owner.getLevel() == this.getLevel() ? owner.getUniqueId() : -1L;
        this.data.set(OWNER_EID, actorId);
    }

    private static @Nullable UUID readOwnerUniqueId(NbtMap tag) {
        if (!(tag.get(OWNER_UUID) instanceof String value)) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
