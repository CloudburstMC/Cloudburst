package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Determines the bucket item produced when a player collects a block.
 */
@FunctionalInterface
public interface BucketPickupHandler {

    /**
     * Returns the filled bucket for a pickup attempt.
     *
     * @param block the block being collected
     * @param player the player collecting the block
     * @return the filled bucket, or {@link ItemStack#EMPTY} when the block cannot be collected
     */
    ItemStack execute(Block block, Player player);
}
