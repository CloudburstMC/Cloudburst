package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Blaze;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityBlaze extends EntityHostile implements Blaze {

    public EntityBlaze(EntityType<Blaze> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.8f;
    }

    @Override
    public String getName() {
        return "Blaze";
    }
}
