package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Armadillo;
import org.cloudburstmc.api.level.Location;

public class EntityArmadillo extends Animal implements Armadillo {

    public EntityArmadillo(EntityType<Armadillo> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(12);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.42f : 0.7f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.39f : 0.65f;
    }

    @Override
    public String getName() {
        return "Armadillo";
    }
}
