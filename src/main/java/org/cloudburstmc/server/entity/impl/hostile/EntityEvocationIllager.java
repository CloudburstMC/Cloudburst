package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.EvocationIllager;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntityEvocationIllager extends EntityHostile implements EvocationIllager {

    public EntityEvocationIllager(EntityType<EvocationIllager> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(24);
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
        return "Evoker";
    }
}
