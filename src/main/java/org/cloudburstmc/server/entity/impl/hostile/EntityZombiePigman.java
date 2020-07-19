package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.hostile.ZombiePigman;
import org.cloudburstmc.server.level.Location;

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
