package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageType;

/**
 * Resolves the damage type used when an entity deals projectile damage.
 */
@FunctionalInterface
public interface DamageTypeEntityHandler {

    /**
     * Resolves the damage type for an entity.
     *
     * @param entity the entity dealing damage
     * @return the damage type
     */
    DamageType execute(Entity entity);
}
