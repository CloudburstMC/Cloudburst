package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

/**
 * Calculates a floating-point property of an entity.
 */
@FunctionalInterface
public interface FloatEntityHandler {

    /**
     * @param entity the entity to query
     * @return the property value
     */
    float execute(Entity entity);
}
