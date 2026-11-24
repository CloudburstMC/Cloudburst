package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Zombie;
import org.cloudburstmc.api.level.Location;

/**
 * Created by Dr. Nick Doran on 4/23/2017.
 */
public class EntityZombie extends EntityHostile implements Zombie {

    public EntityZombie(EntityType<? extends Zombie> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
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
        return "Zombie";
    }
}
