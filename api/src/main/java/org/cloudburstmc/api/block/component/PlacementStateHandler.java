package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Resolves a block state from its placement context.
 */
@FunctionalInterface
public interface PlacementStateHandler {

    /**
     * Resolves the state that will be placed at the selected block.
     *
     * @param state state supplied by the held item
     * @param block selected block
     * @param player player placing the block
     * @param face clicked face
     * @param clickPosition click position within the block
     * @return state to validate and place
     */
    BlockState execute(BlockState state, Block block, Player player, Direction face, Vector3f clickPosition);
}
