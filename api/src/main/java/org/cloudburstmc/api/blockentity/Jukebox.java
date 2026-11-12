package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockSlotGroup;
import org.cloudburstmc.api.item.ItemStack;

/**
 * A jukebox and its single record slot.
 */
public interface Jukebox extends BlockEntity, BlockSlotGroup<Jukebox> {

    /**
     * Returns the stored record, or an empty stack.
     */
    ItemStack getRecord();

    /**
     * Replaces the record and starts playback. An empty stack clears the slot and stops playback.
     *
     * @throws IllegalArgumentException if the item is not playable
     */
    void setRecord(ItemStack record);

    /**
     * Returns whether a song is currently playing.
     */
    boolean isPlaying();

    /**
     * Starts the stored song from the beginning and returns whether playback could start.
     */
    boolean startPlaying();

    /**
     * Stops playback without removing the record.
     */
    void stopPlaying();

    /**
     * Ejects the record into the world and returns whether a record was present.
     */
    boolean eject();

    /**
     * Returns the signal for the stored record, including after playback ends.
     */
    int getComparatorOutput();
}
