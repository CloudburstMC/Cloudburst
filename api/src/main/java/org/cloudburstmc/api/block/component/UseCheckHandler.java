package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;

/**
 * Gate handler that decides whether a block's {@code USE} handler should fire
 * for a given player interaction.
 *
 * <p>Blocks with an interactive UI (containers, etc.) should return {@code false}
 * when the player is sneaking while holding an item, so the item's own placement
 * logic can run instead. Blocks that have no UI (such as the respawn anchor or
 * bed) should return {@code true} unconditionally.</p>
 */
@FunctionalInterface
public interface UseCheckHandler {

    boolean execute(Block block, Player player);
}
