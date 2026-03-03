package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

@FunctionalInterface
public interface FloatEntityHandler {

    float execute(Entity entity);
}
