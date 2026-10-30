package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Ownable;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.TameableData;
import org.cloudburstmc.api.level.Location;

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
    protected void applyData(EntityData data) {
        super.applyData(data);
        TameableData tameableData = (TameableData) data;
        if (tameableData.getOwnerId() != null) {
            this.setOwnerId(tameableData.getOwnerId());
            this.setTamed(true);
        }
        if (tameableData.getSitting() != null) {
            this.setSitting(tameableData.getSitting());
        }
    }

    @Override
    protected void fillData(EntityData data) {
        super.fillData(data);
        TameableData tameableData = (TameableData) data;
        tameableData.setOwnerId(this.getOwnerId());
        tameableData.setSitting(this.isSitting());
    }

    @Override
    public long getOwnerId() {
        return this.data.require(OWNER_EID);
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
