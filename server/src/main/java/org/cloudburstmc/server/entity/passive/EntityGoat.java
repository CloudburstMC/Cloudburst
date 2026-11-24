package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Goat;
import org.cloudburstmc.api.level.Location;

public class EntityGoat extends Animal implements Goat {

    public EntityGoat(EntityType<Goat> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(10);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.315f : 0.63f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.455f : 0.91f;
    }

    @Override
    protected String getDefaultName() {
        return "Goat";
    }
}
