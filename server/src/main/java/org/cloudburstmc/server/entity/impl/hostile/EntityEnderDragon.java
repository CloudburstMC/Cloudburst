package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.EnderDragon;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityEnderDragon extends EntityHostile implements EnderDragon {

    public EntityEnderDragon(EntityType<EnderDragon> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 13f;
    }

    @Override
    public float getHeight() {
        return 4f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(200);
    }

    @Override
    public String getName() {
        return "EnderDragon";
    }
}
