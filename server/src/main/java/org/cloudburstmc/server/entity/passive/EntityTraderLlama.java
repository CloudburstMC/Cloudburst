package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.TraderLlama;
import org.cloudburstmc.api.level.Location;

public class EntityTraderLlama extends Animal implements TraderLlama {

    public EntityTraderLlama(EntityType<TraderLlama> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.45f : 0.9f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.935f : 1.87f;
    }

    @Override
    public String getName() {
        return "Trader Llama";
    }
}
