package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Shulker;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityShulker extends EntityHostile implements Shulker {

    public EntityShulker(EntityType<Shulker> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(30);
    }

    @Override
    public float getWidth() {
        return 1f;
    }

    @Override
    public float getHeight() {
        return 1f;
    }

    @Override
    public String getName() {
        return "Shulker";
    }
}
