package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.misc.FallingBlock;

/**
 * Resolves the damage source used by a falling block when it lands on an entity.
 */
@FunctionalInterface
public interface FallingBlockDamageSourceHandler {

    /**
     * @param entity the falling block causing damage
     * @return the landing damage source
     */
    DamageSource execute(FallingBlock entity);
}
