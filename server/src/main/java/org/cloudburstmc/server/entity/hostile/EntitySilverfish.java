package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Silverfish;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySilverfish extends EntityHostile implements Silverfish {

    public EntitySilverfish(EntityType<Silverfish> type, Location location) {
        super(type, location);
    }

    @Override
    protected String getDefaultName() {
        return "Silverfish";
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
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(8);
    }
}
