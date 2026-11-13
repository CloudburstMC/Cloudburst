package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.DoorHinge;

/**
 * One half of a door, with its facing, hinge and open state.
 * Changes affect only the represented state, not the other half of the door.
 */
public interface Door extends Directional, Bisected, Openable {

    /**
     * @return the hinge side
     */
    DoorHinge getHinge();

    /**
     * @param hinge the replacement hinge side
     * @return a state with the requested hinge
     */
    BlockState withHinge(DoorHinge hinge);
}
