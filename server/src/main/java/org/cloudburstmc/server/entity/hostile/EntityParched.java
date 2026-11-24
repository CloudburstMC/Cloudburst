package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Parched;
import org.cloudburstmc.api.level.Location;

public class EntityParched extends EntityAbstractSkeleton implements Parched {

    public EntityParched(EntityType<Parched> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(16);
    }

    @Override
    public float getHeight() {
        return 1.9f;
    }

    @Override
    protected String getDefaultName() {
        return "Parched";
    }
}
