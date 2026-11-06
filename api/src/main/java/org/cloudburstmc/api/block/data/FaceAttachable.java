package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.AttachedFace;

/**
 * A block mounted on a floor, wall or ceiling.
 */
public interface FaceAttachable extends BlockData {

    /**
     * @return the current mounting surface
     */
    AttachedFace getAttachedFace();

    /**
     * Retains the existing alignment where possible. A wall attachment uses
     * north when the state has no horizontal alignment.
     *
     * @param face the replacement mounting surface
     * @return a state with the requested attachment
     */
    BlockState withAttachedFace(AttachedFace face);
}
