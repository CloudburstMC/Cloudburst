package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Camel;
import org.cloudburstmc.api.level.Location;

public class EntityCamel extends EntityAbstractHorse implements Camel {

    public EntityCamel(EntityType<? extends Camel> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(32);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.85f : 1.7f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 1.1875f : 2.375f;
    }

    @Override
    protected String getDefaultName() {
        return "Camel";
    }
}
