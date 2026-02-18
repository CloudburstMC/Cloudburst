package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Bee;
import org.cloudburstmc.api.level.Location;

public class EntityBee extends Animal implements Bee {

    public EntityBee(EntityType<Bee> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.275f : 0.55f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.25f : 0.5f;
    }

    @Override
    public String getName() {
        return "Bee";
    }
}
