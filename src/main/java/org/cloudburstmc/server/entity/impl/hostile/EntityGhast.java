package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Ghast;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityGhast extends EntityHostile implements Ghast {

    public EntityGhast(EntityType<Ghast> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public float getWidth() {
        return 4;
    }

    @Override
    public float getHeight() {
        return 4;
    }

    @Override
    public String getName() {
        return "Ghast";
    }
}
