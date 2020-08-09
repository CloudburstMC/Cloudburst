package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Salmon;
import org.cloudburstmc.server.level.Location;

/**
 * Created by PetteriM1
 */
public class EntitySalmon extends Animal implements Salmon {

    public EntitySalmon(EntityType<Salmon> type, Location location) {
        super(type, location);
    }

    public String getName() {
        return "Salmon";
    }

    @Override
    public float getWidth() {
        return 0.7f;
    }

    @Override
    public float getHeight() {
        return 0.4f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(3);
    }
}
