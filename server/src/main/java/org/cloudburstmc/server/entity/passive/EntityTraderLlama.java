package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.TraderLlama;
import org.cloudburstmc.api.level.Location;

public class EntityTraderLlama extends EntityLlama implements TraderLlama {

    public EntityTraderLlama(EntityType<TraderLlama> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected String getDefaultName() {
        return "Trader Llama";
    }
}
