package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Determines whether block placement may replace an existing block.
 */
@FunctionalInterface
public interface ReplaceBlockHandler {

    /**
     * Checks whether a placed block may replace the current block.
     *
     * @param block block being replaced
     * @param replacement state being placed
     * @param player player placing the block, or {@code null}
     * @param face clicked face
     * @param clickPosition click position within the block
     * @return whether replacement is allowed
     */
    boolean execute(Block block, BlockState replacement, @Nullable Player player, Direction face, Vector3f clickPosition);
}
