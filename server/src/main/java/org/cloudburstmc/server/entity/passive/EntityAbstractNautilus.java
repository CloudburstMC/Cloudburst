package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;

public abstract class EntityAbstractNautilus extends EntityWaterAnimal {

    protected EntityAbstractNautilus(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(15);
    }

    @Override
    public float getWidth() {
        return 0.875f;
    }

    @Override
    public float getHeight() {
        return 0.95f;
    }
}
