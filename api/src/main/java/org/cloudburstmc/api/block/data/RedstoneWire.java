package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.RedstoneSide;

/**
 * Redstone wire with a signal strength and four horizontal connection shapes.
 */
public interface RedstoneWire extends AnaloguePowerable {

    /**
     * @param face the horizontal direction to read
     * @return the connection shape
     * @throws IllegalArgumentException if the direction is vertical
     */
    RedstoneSide getFace(Direction face);

    /**
     * @param face       the horizontal direction to change
     * @param connection the replacement connection shape
     * @return a state with the requested connection
     * @throws IllegalArgumentException if the direction is vertical
     */
    BlockState withFace(Direction face, RedstoneSide connection);
}
