package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Ravager;
import org.cloudburstmc.server.level.Location;

public class EntityRavager extends EntityHostile implements Ravager {

    public EntityRavager(EntityType<Ravager> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(100);
    }

    @Override
    public float getHeight() {
        return 1.9f;
    }

    @Override
    public float getWidth() {
        return 1.2f;
    }

    @Override
    public String getName() {
        return "Ravager";
    }
}
