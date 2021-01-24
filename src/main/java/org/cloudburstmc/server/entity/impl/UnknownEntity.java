package org.cloudburstmc.server.entity.impl;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.player.Player;

public class UnknownEntity extends BaseEntity {
    public UnknownEntity(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public void spawnTo(Player player) {
        // no-op
    }

    @Override
    public void despawnFrom(Player player) {
        // no-op
    }
}
