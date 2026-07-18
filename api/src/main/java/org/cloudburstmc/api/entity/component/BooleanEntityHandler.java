package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;

/**
 * Evaluates a boolean property of an entity.
 */
@FunctionalInterface
public interface BooleanEntityHandler {

    /**
     * @param entity the entity to evaluate
     * @return the property value
     */
    boolean execute(Entity entity);
}
