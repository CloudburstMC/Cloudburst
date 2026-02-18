package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Sniffer;
import org.cloudburstmc.api.level.Location;

public class EntitySniffer extends Animal implements Sniffer {

    public EntitySniffer(EntityType<Sniffer> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(14);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.855f : 1.9f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.7875f : 1.75f;
    }

    @Override
    public String getName() {
        return "Sniffer";
    }
}
