package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.SulfurCube;
import org.cloudburstmc.api.level.Location;

public class EntitySulfurCube extends Animal implements SulfurCube {

    public EntitySulfurCube(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(9);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.49f : 0.98f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.49f : 0.98f;
    }

    @Override
    public String getName() {
        return "Sulfur Cube";
    }
}
