package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Cod;
import org.cloudburstmc.server.world.Location;

/**
 * Created by PetteriM1
 */
public class EntityCod extends Animal implements Cod {

    public EntityCod(EntityType<Cod> type, Location location) {
        super(type, location);
    }

    public String getName() {
        return "Cod";
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.2f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(3);
    }
}
