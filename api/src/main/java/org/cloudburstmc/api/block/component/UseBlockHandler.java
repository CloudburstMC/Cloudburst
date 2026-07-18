package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

/**
 * Handles a player using a block.
 */
@FunctionalInterface
public interface UseBlockHandler {

    /**
     * @param block     the block that was clicked
     * @param player    the interacting player, or {@code null} for non-player sources
     * @param direction the face that was clicked
     * @param item      the item held by the player at the time of interaction,
     *                  or {@link ItemStack#EMPTY} when invoked without a player context
     * @return {@code true} if the interaction was consumed
     */
    boolean execute(Block block, Player player, Direction direction, ItemStack item);
}
