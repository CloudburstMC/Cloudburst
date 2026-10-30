package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Ownable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.Set;

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
    protected void loadCustom(NbtMap tag) {
        // Migrated from applyData: runs before the living-state load below,
        // matching the old applyData-before-loadCustom order. Only a numeric
        // value tames, exactly as the old DTO null-check did: a non-numeric
        // value bound to null and left everything alone.
        if (tag.get("OwnerID") instanceof Number) {
            this.setOwnerId(getLong(tag, "OwnerID", this.getOwnerId()));
            this.setTamed(true);
        }
        if (tag.containsKey("Sitting")) {
            this.setSitting(getBoolean(tag, "Sitting", this.isSitting()));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putLong("OwnerID", this.getOwnerId());
        tag.putBoolean("Sitting", this.isSitting());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("OwnerID");
        keys.add("Sitting");
        return Set.copyOf(keys);
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
