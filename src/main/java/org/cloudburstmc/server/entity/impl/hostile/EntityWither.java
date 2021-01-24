package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.hostile.Wither;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntityWither extends EntityHostile implements Wither, Smiteable {

    public EntityWither(EntityType<Wither> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        return 3.5f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(300);
    }

    @Override
    public String getName() {
        return "Wither";
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}
