package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.Arthropod;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.CaveSpider;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityCaveSpider extends EntityHostile implements CaveSpider, Arthropod {

    public EntityCaveSpider(EntityType<CaveSpider> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(12);
    }

    @Override
    public float getWidth() {
        return 0.7f;
    }

    @Override
    public float getHeight() {
        return 0.5f;
    }

    @Override
    public String getName() {
        return "CaveSpider";
    }
}
