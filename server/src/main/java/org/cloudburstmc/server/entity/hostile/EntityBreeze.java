package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Breeze;
import org.cloudburstmc.api.level.Location;

public class EntityBreeze extends EntityHostile implements Breeze {

    public EntityBreeze(EntityType<Breeze> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(30);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.77f;
    }

    @Override
    public String getName() {
        return "Breeze";
    }
}
