package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.hostile.WitherSkeleton;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityWitherSkeleton extends EntityHostile implements WitherSkeleton, Smiteable {

    public EntityWitherSkeleton(EntityType<WitherSkeleton> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
    }

    @Override
    public float getWidth() {
        return 0.7f;
    }

    @Override
    public float getHeight() {
        return 2.4f;
    }

    @Override
    public String getName() {
        return "WitherSkeleton";
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}
