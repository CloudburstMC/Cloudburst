package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

import java.util.Set;

/**
 * A chiseled bookshelf's facing and visible occupied slots.
 * Changing these flags does not change its inventory contents.
 * Slots are numbered left to right across the top row, then the bottom row.
 */
public interface ChiseledBookshelf extends Directional {

    /**
     * @return the number of slots
     */
    int getSlotCount();

    /**
     * @param slot the zero-based slot to read
     * @return whether the slot is marked as occupied
     * @throws IllegalArgumentException if the slot is outside the supported range
     */
    boolean isSlotOccupied(int slot);

    /**
     * @return the immutable set of occupied slot indices
     */
    Set<Integer> getOccupiedSlots();

    /**
     * @param slot     the zero-based slot to change
     * @param occupied whether the slot is occupied
     * @return a state with the requested slot flag
     * @throws IllegalArgumentException if the slot is outside the supported range
     */
    BlockState withSlotOccupied(int slot, boolean occupied);
}
