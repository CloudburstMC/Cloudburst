package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.HangingData;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class HangingEntity extends CloudEntity {

    public HangingEntity(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setMaxHealth(1);
        this.setHealth(1);
    }

    @Override
    protected HangingData data() {
        return (HangingData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return HangingData.class;
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putInt("TileX", (int) this.getX());
        tag.putInt("TileY", (int) this.getY());
        tag.putInt("TileZ", (int) this.getZ());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.addAll(Set.of("TileX", "TileY", "TileZ"));
        return Set.copyOf(keys);
    }

    @Override
    public Direction getDirection() {
        return this.data().getDirection();
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (!this.isAlive()) {

            this.despawnFromAll();
            if (!this.isPlayer) {
                this.close();
            }

            return true;
        }

        if (this.lastYaw != this.getY() || !this.position.equals(this.lastPosition)) {
            this.despawnFromAll();

            this.data().setDirection(Direction.fromIndex((byte) (this.getYaw() / 90)));

            this.lastYaw = this.getYaw();
            this.lastPosition = this.position;

            this.spawnToAll();

            return true;
        }

        return false;
    }

    protected boolean isSurfaceValid() {
        return true;
    }

}
