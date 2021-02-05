package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.player.CloudPlayer;

public class UnknownEntity extends BaseEntity {
    public UnknownEntity(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        // no-op
    }

    @Override
    public void despawnFrom(CloudPlayer player) {
        // no-op
    }
}
