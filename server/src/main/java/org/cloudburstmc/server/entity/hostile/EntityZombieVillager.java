package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.ZombieVillager;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityZombieVillager extends EntityZombie implements ZombieVillager {

    public EntityZombieVillager(EntityType<ZombieVillager> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected String getDefaultName() {
        return "Zombie Villager";
    }
}
