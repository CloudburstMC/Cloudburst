package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Husk;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityHusk extends EntityZombie implements Husk {

    public EntityHusk(EntityType<Husk> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected String getDefaultName() {
        return "Husk";
    }
}
