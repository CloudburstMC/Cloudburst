package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.DeprecatedZombieVillager;
import org.cloudburstmc.api.level.Location;

public class EntityDeprecatedZombieVillager extends EntityHostile implements DeprecatedZombieVillager {

    public EntityDeprecatedZombieVillager(EntityType<DeprecatedZombieVillager> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.95f;
    }

    @Override
    protected String getDefaultName() {
        return "Zombie Villager";
    }
}
