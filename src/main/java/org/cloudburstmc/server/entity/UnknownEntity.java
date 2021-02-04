package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.server.level.Location;
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
