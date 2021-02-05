package org.cloudburstmc.server.entity;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.math.Direction;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class HangingEntity extends BaseEntity {
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
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForByte("Direction", v -> this.direction = v);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putByte("Direction", this.direction);
        tag.putInt("TileX", (int) this.getX());
        tag.putInt("TileY", (int) this.getY());
        tag.putInt("TileZ", (int) this.getZ());
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
