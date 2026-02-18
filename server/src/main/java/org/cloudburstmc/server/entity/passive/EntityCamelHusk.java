package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.CamelHusk;
import org.cloudburstmc.api.level.Location;

public class EntityCamelHusk extends Animal implements CamelHusk {

    public EntityCamelHusk(EntityType<CamelHusk> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(32);
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
    public String getName() {
        return "Camel Husk";
    }
}
