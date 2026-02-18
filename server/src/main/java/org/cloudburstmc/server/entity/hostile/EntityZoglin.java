package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Zoglin;
import org.cloudburstmc.api.level.Location;

public class EntityZoglin extends EntityHostile implements Zoglin {

    public EntityZoglin(EntityType<Zoglin> type, Location location) {
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
        return "Zoglin";
    }
}
