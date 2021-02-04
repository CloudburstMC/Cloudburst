package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Vex;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityVex extends EntityHostile implements Vex {

    public EntityVex(EntityType<Vex> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(14);
    }

    @Override
    public float getWidth() {
        return 0.4f;
    }

    @Override
    public float getHeight() {
        return 0.8f;
    }

    @Override
    public String getName() {
        return "Vex";
    }
}
