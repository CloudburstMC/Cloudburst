package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

@FunctionalInterface
public interface BooleanEntityHandler {

    boolean execute(Entity entity);
}
