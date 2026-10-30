package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.HangingData;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Set;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class HangingEntity extends CloudEntity {
    protected byte direction;

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
    protected void applyData(EntityData data) {
        super.applyData(data);
        HangingData hangingData = (HangingData) data;
        if (hangingData.getDirection() != null) {
            this.direction = hangingData.getDirection().byteValue();
        }
    }

    @Override
    protected void fillData(EntityData data) {
        super.fillData(data);
        ((HangingData) data).setDirection((int) this.direction);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        tag.putInt("TileX", (int) this.getX());
        tag.putInt("TileY", (int) this.getY());
        tag.putInt("TileZ", (int) this.getZ());
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("TileX", "TileY", "TileZ");
    }

    @Override
    public Direction getDirection() {
        return Direction.fromIndex(this.direction);
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

            this.direction = (byte) (this.getYaw() / 90);

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
