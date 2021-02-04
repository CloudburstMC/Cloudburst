package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Panda;
import org.cloudburstmc.server.level.Location;

public class EntityPanda extends Animal implements Panda {

    public EntityPanda(EntityType<Panda> type, Location location) {
        super(type, location);
    }

    @Override
    public float getLength() {
        return 1.825f;
    }

    @Override
    public float getWidth() {
        return 1.125f;
    }

    @Override
    public float getHeight() {
        return 1.25f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }
}
