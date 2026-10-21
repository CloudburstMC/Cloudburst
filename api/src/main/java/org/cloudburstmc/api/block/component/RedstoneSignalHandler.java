package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.Direction;

/**
 * Resolves the redstone signal emitted through a block face.
 */
@FunctionalInterface
public interface RedstoneSignalHandler {

    /**
     * @param block the signal source
     * @param face  the face through which the signal is emitted
     * @return signal strength from {@code 0} to {@code 15}
     */
    int execute(Block block, Direction face);
}
