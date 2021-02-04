package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Pufferfish;
import org.cloudburstmc.server.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityPufferfish extends Animal implements Pufferfish {

    public EntityPufferfish(EntityType<Pufferfish> type, Location location) {
        super(type, location);
    }

    public String getName() {
        return "Pufferfish";
    }

    @Override
    public float getWidth() {
        return 0.35f;
    }

    @Override
    public float getHeight() {
        return 0.35f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(3);
    }
}
