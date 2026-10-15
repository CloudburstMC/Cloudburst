package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;

/**
 * A frame containing at most one displayed item.
 */
public interface ItemFrame extends BlockEntity {

    /**
     * @return the outward-facing direction
     */
    Direction getFacing();

    /**
     * @return the clockwise rotation step, from zero through seven
     */
    int getItemRotation();

    /**
     * @param itemRotation the clockwise rotation step, from zero through seven
     * @throws IllegalArgumentException if the rotation is outside this range
     */
    void setItemRotation(int itemRotation);

    /**
     * @return the displayed item, or an empty stack
     */
    ItemStack getItem();

    /**
     * Sets the displayed item without consuming inventory items or firing an interaction event.
     * Non-empty stacks are reduced to one item. Clearing the frame resets its rotation.
     *
     * @param item the displayed item, or an empty stack
     */
    void setItem(ItemStack item);

    /**
     * @return the probability of dropping the displayed item when it is removed or the frame is broken
     */
    float getItemDropChance();

    /**
     * @param itemDropChance a finite probability from zero through one
     * @throws IllegalArgumentException if the probability is non-finite or outside this range
     */
    void setItemDropChance(float itemDropChance);

    /**
     * @return whether ordinary interactions and support loss leave this frame unchanged
     */
    boolean isFixed();

    /**
     * Fixed frames can still be changed through this API.
     *
     * @param fixed whether to prevent ordinary changes and support-based removal
     */
    void setFixed(boolean fixed);

    /**
     * @return zero for an empty frame, otherwise the rotation step plus one
     */
    int getAnalogOutput();
}
