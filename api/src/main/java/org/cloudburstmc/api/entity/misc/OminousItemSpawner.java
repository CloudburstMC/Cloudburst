package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * A temporary entity that displays and releases an item.
 */
public interface OminousItemSpawner extends Entity {

    /**
     * Returns the item to release, or {@link ItemStack#EMPTY} if none is set.
     */
    ItemStack getItem();

    /**
     * Sets the item to release. An empty stack clears the item.
     */
    void setItem(ItemStack item);

    /**
     * Returns the remaining ticks before the item is released.
     */
    int getTicksUntilSpawn();

    /**
     * Sets the remaining delay. Zero releases the item on the next tick.
     *
     * @throws IllegalArgumentException if the delay is negative
     */
    void setTicksUntilSpawn(int ticks);
}
