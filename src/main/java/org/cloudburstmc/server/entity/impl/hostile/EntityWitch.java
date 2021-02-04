package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Witch;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityWitch extends EntityHostile implements Witch {

    public EntityWitch(EntityType<Witch> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(26);
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
        return "Witch";
    }
}
