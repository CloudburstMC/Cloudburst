package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.BlockHitResult;

/**
 * Handles a projectile striking a block.
 */
@FunctionalInterface
public interface ProjectileHitBlockHandler {

    /**
     * @param hit        the point and face struck
     * @param projectile the projectile that struck the block
     */
    void execute(BlockHitResult hit, Entity projectile);
}
