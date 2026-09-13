package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

/**
 * Handles a player attacking a block.
 */
@FunctionalInterface
public interface AttackBlockHandler {

    /**
     * @param block     the attacked block
     * @param player    the attacking player
     * @param direction the attacked face
     * @return whether the attack was consumed
     */
    boolean execute(Block block, Player player, Direction direction);
}
