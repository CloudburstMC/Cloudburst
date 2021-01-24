package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.MagmaCube;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntityMagmaCube extends EntityHostile implements MagmaCube {

    public EntityMagmaCube(EntityType<MagmaCube> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(16);
    }

    @Override
    public float getWidth() {
        return 2.04f;
    }

    @Override
    public float getHeight() {
        return 2.04f;
    }

    @Override
    public String getName() {
        return "Magma Cube";
    }
}
