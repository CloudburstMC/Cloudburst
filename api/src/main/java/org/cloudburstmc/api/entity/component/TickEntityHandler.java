package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

/**
 * Runs after a live entity's normal tick.
 */
@FunctionalInterface
public interface TickEntityHandler {

    /**
     * @param entity the entity being ticked
     * @param currentTick the current tick
     * @return whether to request another tick even if normal behavior does not need one
     */
    boolean execute(Entity entity, int currentTick);
}
