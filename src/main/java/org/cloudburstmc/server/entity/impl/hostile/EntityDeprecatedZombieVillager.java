package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.hostile.DeprecatedZombieVillager;
import org.cloudburstmc.server.world.Location;

public class EntityDeprecatedZombieVillager extends EntityHostile implements DeprecatedZombieVillager, Smiteable {

    public EntityDeprecatedZombieVillager(EntityType<DeprecatedZombieVillager> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
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
    public String getName() {
        return "Zombie Villager";
    }
}
