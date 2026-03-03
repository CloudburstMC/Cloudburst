package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

@FunctionalInterface
public interface TickEntityHandler {

    boolean execute(Entity entity, int currentTick);
}
