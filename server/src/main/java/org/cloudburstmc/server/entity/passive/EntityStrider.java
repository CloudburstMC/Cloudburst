package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Strider;
import org.cloudburstmc.api.level.Location;

public class EntityStrider extends Animal implements Strider {

    public EntityStrider(EntityType<Strider> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.45f : 0.9f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.85f : 1.7f;
    }

    @Override
    public String getName() {
        return "Strider";
    }
}
