package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Pillager;
import org.cloudburstmc.server.level.Location;

public class EntityPillager extends EntityHostile implements Pillager {

    public EntityPillager(EntityType<Pillager> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(24);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.95f;
    }

    @Override
    public String getName() {
        return "Pillager";
    }
}
