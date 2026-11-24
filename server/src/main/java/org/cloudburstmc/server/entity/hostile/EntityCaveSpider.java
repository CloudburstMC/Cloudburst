package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.CaveSpider;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityCaveSpider extends EntitySpider implements CaveSpider {

    public EntityCaveSpider(EntityType<CaveSpider> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(12);
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
    protected String getDefaultName() {
        return "CaveSpider";
    }
}
