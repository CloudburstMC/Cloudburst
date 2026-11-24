package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Evoker;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityEvoker extends EntityHostile implements Evoker {

    public EntityEvoker(EntityType<Evoker> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(24);
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
    protected String getDefaultName() {
        return "Evoker";
    }
}
