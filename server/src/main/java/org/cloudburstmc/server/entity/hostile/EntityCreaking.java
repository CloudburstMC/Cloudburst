package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Creaking;
import org.cloudburstmc.api.level.Location;

public class EntityCreaking extends EntityHostile implements Creaking {

    public EntityCreaking(EntityType<Creaking> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(1);
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        return 2.7f;
    }

    @Override
    public String getName() {
        return "Creaking";
    }
}
