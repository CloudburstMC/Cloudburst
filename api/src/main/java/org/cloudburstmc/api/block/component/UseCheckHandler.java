package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;

/**
 * Determines whether a block may handle a player interaction.
 */
@FunctionalInterface
public interface UseCheckHandler {

    /**
     * @param block the block being used
     * @param player the interacting player
     * @return whether the block may handle the interaction
     */
    boolean execute(Block block, Player player);
}
