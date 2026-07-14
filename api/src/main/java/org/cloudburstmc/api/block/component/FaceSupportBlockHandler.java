package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.util.Direction;

/**
 * Tests whether a block face can support another block.
 */
@FunctionalInterface
public interface FaceSupportBlockHandler {

    /**
     * Checks support for a face of a block state.
     *
     * @param state the block state
     * @param direction the face being tested
     * @param supportType the support area required
     * @return {@code true} if the face provides the requested support
     */
    boolean execute(BlockState state, Direction direction, SupportType supportType);
}
