package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.Arthropod;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Silverfish;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySilverfish extends EntityHostile implements Silverfish, Arthropod {

    public EntitySilverfish(EntityType<Silverfish> type, Location location) {
        super(type, location);
    }

    @Override
    public String getName() {
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
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(8);
    }
}
