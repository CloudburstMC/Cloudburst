package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.Arthropod;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Endermite;
import org.cloudburstmc.api.level.Location;

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
