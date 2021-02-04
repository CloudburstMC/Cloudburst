package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.EvocationIllager;
import org.cloudburstmc.server.level.Location;

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
