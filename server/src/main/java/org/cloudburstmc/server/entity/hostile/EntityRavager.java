package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Ravager;
import org.cloudburstmc.api.level.Location;

public class EntityRavager extends EntityHostile implements Ravager {

    public EntityRavager(EntityType<Ravager> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(100);
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
    protected String getDefaultName() {
        return "Ravager";
    }
}
