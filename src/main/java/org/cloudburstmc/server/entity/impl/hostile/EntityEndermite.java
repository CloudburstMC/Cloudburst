package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.Arthropod;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Endermite;
import org.cloudburstmc.server.world.Location;

/**
 * @author Box.
 */
public class EntityEndermite extends EntityHostile implements Endermite, Arthropod {

    public EntityEndermite(EntityType<Endermite> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(8);
    }

    @Override
    public float getWidth() {
        return 0.4f;
    }

    @Override
    public float getHeight() {
        return 0.3f;
    }

    @Override
    public String getName() {
        return "Endermite";
    }
}
