package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.BedPart;

/**
 * One part of a bed, with its facing and occupied state.
 * Changes affect only the represented state, not the other part of the bed.
 */
public interface Bed extends Directional {

    /**
     * @return the represented bed part
     */
    BedPart getPart();

    /**
     * @param part the replacement bed part
     * @return a state representing the requested part
     */
    BlockState withPart(BedPart part);

    /**
     * @return whether the bed is marked as occupied
     */
    boolean isOccupied();

    /**
     * Changes the occupied flag without starting or ending a player's sleep.
     *
     * @param occupied the replacement occupied state
     * @return a state with the requested occupied flag
     */
    BlockState withOccupied(boolean occupied);
}
