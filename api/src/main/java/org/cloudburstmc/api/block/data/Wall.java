package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.WallConnectionType;

/**
 * A wall's central post and four horizontal connection heights.
 */
public interface Wall extends BlockData {

    /**
     * @return whether the central post is present
     */
    boolean isUp();

    /**
     * @param up whether the central post is present
     * @return a state with the requested post value
     */
    BlockState withUp(boolean up);

    /**
     * @param face the horizontal direction to read
     * @return the connection height, or {@link WallConnectionType#NONE} when absent
     * @throws IllegalArgumentException if the direction is vertical
     */
    WallConnectionType getHeight(Direction face);

    /**
     * @param face   the horizontal direction to change
     * @param height the replacement connection height
     * @return a state with the requested connection height
     * @throws IllegalArgumentException if the direction is vertical
     */
    BlockState withHeight(Direction face, WallConnectionType height);
}
