package org.cloudburstmc.server.entity.impl.passive;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.entity.EntityOwnable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.server.level.Location;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.OWNER_EID;
import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.SITTING;
import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.TAMED;

/**
 * Author: BeYkeRYkt
 * Nukkit Project
 */
public abstract class EntityTameable extends Animal implements EntityOwnable {


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
        return this.data.getLong(OWNER_EID);
    }

    @Override
    public void setOwnerId(long id) {
        this.data.setLong(OWNER_EID, id);
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
