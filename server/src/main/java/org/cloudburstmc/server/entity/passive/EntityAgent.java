package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Agent;
import org.cloudburstmc.api.entity.passive.Allay;
import org.cloudburstmc.api.level.Location;

public class EntityAgent extends Animal implements Agent {

    public EntityAgent(EntityType<Agent> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(5);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 0.93f;
    }

    @Override
    public String getName() {
        return "Agent";
    }
}
