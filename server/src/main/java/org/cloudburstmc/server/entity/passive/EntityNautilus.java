package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Nautilus;
import org.cloudburstmc.api.level.Location;

public class EntityNautilus extends EntityWaterAnimal implements Nautilus {

    public EntityNautilus(EntityType<Nautilus> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.44f : 0.875f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.5f : 0.95f;
    }

    @Override
    public String getName() {
        return "Nautilus";
    }
}
