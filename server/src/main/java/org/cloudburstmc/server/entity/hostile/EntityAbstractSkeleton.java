package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.AbstractSkeleton;
import org.cloudburstmc.api.level.Location;

public abstract class EntityAbstractSkeleton extends EntityHostile implements AbstractSkeleton {

    protected EntityAbstractSkeleton(EntityType<? extends AbstractSkeleton> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }
}
