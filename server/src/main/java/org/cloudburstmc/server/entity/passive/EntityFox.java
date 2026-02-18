package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Fox;
import org.cloudburstmc.api.level.Location;

public class EntityFox extends Animal implements Fox {

    public EntityFox(EntityType<Fox> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.3f : 0.6f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.35f : 0.7f;
    }

    @Override
    public String getName() {
        return "Fox";
    }
}
