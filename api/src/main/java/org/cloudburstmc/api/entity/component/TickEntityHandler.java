package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

/**
 * Handles an entity tick.
 */
@FunctionalInterface
public interface TickEntityHandler {

    /**
     * @param entity the entity being ticked
     * @param currentTick the current tick
     * @return whether the entity should remain scheduled for updates
     */
    boolean execute(Entity entity, int currentTick);
}
