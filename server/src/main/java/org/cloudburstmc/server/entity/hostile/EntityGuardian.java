package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Guardian;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityGuardian extends EntityHostile implements Guardian {

    public EntityGuardian(EntityType<? extends Guardian> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(30);
    }

    @Override
    protected String getDefaultName() {
        return "Guardian";
    }

    @Override
    public float getWidth() {
        return 0.85f;
    }

    @Override
    public float getHeight() {
        return 0.85f;
    }
}
