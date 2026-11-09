package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.Direction;

/**
 * Determines whether a candidate block accepts a redstone wire connection.
 * Connection eligibility is independent of the signal currently emitted by the block.
 */
@FunctionalInterface
public interface RedstoneConnectionHandler {

    /**
     * Checks whether the block accepts a connection from redstone wire.
     *
     * @param block     the block to connect to
     * @param direction direction from the wire to the candidate for a horizontal connection,
     *                  or {@code null} when checking above or below an adjacent block
     * @return whether the candidate accepts the connection
     */
    boolean execute(Block block, @Nullable Direction direction);
}
