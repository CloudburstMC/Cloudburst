package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Handles placement of a block state in a level.
 */
@FunctionalInterface
public interface PlaceBlockHandler {

    /**
     * @param blockState the block state being placed
     * @param player the player placing the block
     * @param blockPosition the placement position
     * @param face the clicked face
     * @param clickPosition the click position within the block
     * @return whether placement succeeded
     */
    boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition);
}
