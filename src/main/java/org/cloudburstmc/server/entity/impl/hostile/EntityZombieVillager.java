package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.hostile.ZombieVillager;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntityZombieVillager extends EntityHostile implements ZombieVillager, Smiteable {

    public EntityZombieVillager(EntityType<ZombieVillager> type, Location location) {
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

    @Override
    public boolean isUndead() {
        return true;
    }

}
