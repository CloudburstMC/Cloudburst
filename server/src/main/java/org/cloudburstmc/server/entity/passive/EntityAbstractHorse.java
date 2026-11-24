package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.AbstractHorse;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.data.CloudAgeableMobData;

public abstract class EntityAbstractHorse extends EntityAgeableAnimal implements AbstractHorse {

    protected EntityAbstractHorse(EntityType<? extends AbstractHorse> type, Location location) {
        super(type, location, new CloudAgeableMobData(), false);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(15);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.6982f : 1.3965f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.8f : 1.6f;
    }
}
