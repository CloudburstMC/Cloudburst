package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.WanderingTrader;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityCreature;

public class EntityWanderingTrader extends EntityCreature implements WanderingTrader {

    public EntityWanderingTrader(EntityType<WanderingTrader> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.8f;
    }

    @Override
    public String getName() {
        return "Wandering Trader";
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }
}
