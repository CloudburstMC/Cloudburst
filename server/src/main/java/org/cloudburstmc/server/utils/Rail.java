package org.cloudburstmc.server.utils;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockType;

import java.util.Objects;

import static org.cloudburstmc.server.block.BlockTypes.*;

/**
 * INTERNAL helper class of railway
 * <p>
 * By lmlstarqaq http://snake1999.com/
 * Creation time: 2017/7/1 17:42.
 */
public final class Rail {

    public static boolean isRailBlock(BlockState blockState) {
        Objects.requireNonNull(blockState, "Rail block predicate can not accept null block");
        return isRailBlock(blockState.getType());
    }

    public static boolean isRailBlock(BlockType id) {
        return id == RAIL || id == GOLDEN_RAIL || id == ACTIVATOR_RAIL || id == DETECTOR_RAIL;
    }

    private Rail() {
        //no instance
    }
}
