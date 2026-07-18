package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;

/**
 * Handles an interaction between a player and a block.
 */
@FunctionalInterface
public interface PlayerBlockHandler {

    /**
     * @param block the participating block
     * @param player the participating player
     */
    void execute(Block block, Player player);
}
