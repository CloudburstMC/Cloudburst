package org.cloudburstmc.server.entity;

import org.cloudburstmc.server.world.Location;

@FunctionalInterface
public interface EntityFactory<T extends Entity> {

    T create(EntityType<T> type, Location location);
}
