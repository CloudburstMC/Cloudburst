package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.CopperGolem;
import org.cloudburstmc.api.level.Location;

public class EntityCopperGolem extends Animal implements CopperGolem {

    public EntityCopperGolem(EntityType<CopperGolem> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(12);
    }

    @Override
    public float getWidth() {
        return 0.49f;
    }

    @Override
    public float getHeight() {
        return 0.98f;
    }

    @Override
    public String getName() {
        return "Copper Golem";
    }
}
