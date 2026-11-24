package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * An item stack lying in the world.
 */
public interface DroppedItem extends Entity {

    /**
     * Returns the stack available for pickup.
     */
    ItemStack getItem();

    /**
     * Replaces the stack available for pickup.
     *
     * @throws IllegalArgumentException if the stack is empty
     */
    void setItem(@NonNull ItemStack item);

    /**
     * Returns the remaining pickup delay in ticks, or 32767 if pickup is disabled.
     */
    @NonNegative
    int getPickupDelay();

    /**
     * Sets the pickup delay in ticks. A value of 32767 or more prevents pickup indefinitely.
     */
    void setPickupDelay(@NonNegative int pickupDelay);
}
