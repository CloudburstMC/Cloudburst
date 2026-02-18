package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Allay;
import org.cloudburstmc.api.level.Location;

public class EntityAllay extends Animal implements Allay {

    public EntityAllay(EntityType<Allay> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.35f;
    }

    @Override
    public float getHeight() {
        return 0.6f;
    }

    @Override
    public String getName() {
        return "Allay";
    }
}
