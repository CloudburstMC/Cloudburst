package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Smiteable;
import org.cloudburstmc.api.entity.hostile.ZombiePigman;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityZombiePigman extends EntityHostile implements ZombiePigman, Smiteable {

    public EntityZombiePigman(EntityType<ZombiePigman> type, Location location) {
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
        return "ZombiePigman";
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}
