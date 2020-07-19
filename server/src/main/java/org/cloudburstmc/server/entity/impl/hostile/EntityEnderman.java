package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Enderman;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityEnderman extends EntityHostile implements Enderman {

    public EntityEnderman(EntityType<Enderman> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(40);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 2.9f;
    }

    @Override
    public String getName() {
        return "Enderman";
    }
}
