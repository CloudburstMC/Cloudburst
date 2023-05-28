package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.level.Location;

@FunctionalInterface
public interface EntityFactory<T extends Entity> {

    T create(EntityType<T> type, Location location);
}
