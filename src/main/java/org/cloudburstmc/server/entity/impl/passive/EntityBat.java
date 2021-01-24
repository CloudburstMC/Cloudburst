package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Bat;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntityBat extends Animal implements Bat {

    public EntityBat(EntityType<Bat> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.9f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(6);
    }
}
