package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockChange;
import org.cloudburstmc.api.block.BlockState;

import java.util.List;

/**
 * Proposes the block changes needed for a falling block to settle.
 */
@FunctionalInterface
public interface FallingBlockPlacementHandler {

    /**
     * Do not modify the level. Return at most one change per position in the target's
     * level, with supporting blocks before any blocks that depend on them.
     *
     * @param state  the falling block state
     * @param target the block at the landing position
     * @return the proposed changes in placement order, or an empty list if the block cannot settle
     */
    List<BlockChange> execute(BlockState state, Block target);
}
