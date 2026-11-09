package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;

import java.util.Set;

/**
 * A block with independently enabled faces or connections.
 */
public interface MultipleFacing extends BlockData {

    /**
     * @param face the face to read
     * @return whether the face is enabled
     * @throws IllegalArgumentException if the face is not supported
     */
    boolean hasFace(Direction face);

    /**
     * @return the immutable set of enabled faces
     */
    Set<Direction> getFaces();

    /**
     * @return the immutable set of faces supported by this block
     */
    Set<Direction> getAllowedFaces();

    /**
     * @param face    the face to change
     * @param enabled whether the face is enabled
     * @return a state with the requested face value
     * @throws IllegalArgumentException if the face is not supported
     */
    BlockState withFace(Direction face, boolean enabled);
}
