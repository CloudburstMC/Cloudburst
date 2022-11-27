package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Ownable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.OWNER_EID;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.SITTING;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.TAMED;

/**
 * Author: BeYkeRYkt
 * Nukkit Project
 */
public abstract class EntityTameable extends Animal implements Ownable {


    public EntityTameable(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setOwner(null);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForLong("OwnerID", v -> {
            this.setOwnerId(v);
            this.setTamed(true);
        });
        tag.listenForBoolean("Sitting", this::setSitting);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putLong("OwnerID", this.getOwnerId());
        tag.putBoolean("Sitting", this.isSitting());
    }

    @Override
    public long getOwnerId() {
        return this.data.get(OWNER_EID);
    }

    @Override
    public void setOwnerId(long id) {
        this.data.set(OWNER_EID, id);
    }

    @Override
    public String getName() {
        return getNameTag();
    }

    public boolean isTamed() {
        return this.data.getFlag(TAMED);
    }

    public void setTamed(boolean value) {
        this.data.setFlag(TAMED, value);
    }

    public boolean isSitting() {
        return this.data.getFlag(SITTING);
    }

    public void setSitting(boolean value) {
        this.data.setFlag(SITTING, value);
    }
}
