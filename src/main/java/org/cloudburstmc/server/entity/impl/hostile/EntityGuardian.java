package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Guardian;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityGuardian extends EntityHostile implements Guardian {

    public EntityGuardian(EntityType<Guardian> type, Location location) {
        super(type, location);
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(30);
    }

    @Override
    public String getName() {
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
