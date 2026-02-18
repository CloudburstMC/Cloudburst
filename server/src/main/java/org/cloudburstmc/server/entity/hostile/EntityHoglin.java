package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Hoglin;
import org.cloudburstmc.api.level.Location;

public class EntityHoglin extends EntityHostile implements Hoglin {

    public EntityHoglin(EntityType<Hoglin> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(40);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.6982f : 1.3965f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.7f : 1.4f;
    }

    @Override
    public String getName() {
        return "Hoglin";
    }
}
