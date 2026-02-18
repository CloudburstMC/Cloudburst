package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.ZombieNautilus;
import org.cloudburstmc.api.level.Location;

public class EntityZombieNautilus extends EntityWaterAnimal implements ZombieNautilus {

    public EntityZombieNautilus(EntityType<ZombieNautilus> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public float getWidth() {
        return 0.875f;
    }

    @Override
    public float getHeight() {
        return 0.95f;
    }

    @Override
    public String getName() {
        return "Zombie Nautilus";
    }
}
