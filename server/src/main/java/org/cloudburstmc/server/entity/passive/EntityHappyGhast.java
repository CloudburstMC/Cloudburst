package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.HappyGhast;
import org.cloudburstmc.api.level.Location;

public class EntityHappyGhast extends Animal implements HappyGhast {

    public EntityHappyGhast(EntityType<HappyGhast> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 4.0f;
    }

    @Override
    public float getHeight() {
        return 4.0f;
    }

    @Override
    public String getName() {
        return "Happy Ghast";
    }
}
