package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A standard or tipped arrow that carries the item returned on pickup.
 */
public interface Arrow extends AbstractArrow {

    /**
     * Returns the single arrow item carried by this projectile, including its potion data.
     * This item is returned when the arrow is picked up.
     *
     * @return the carried arrow
     */
    ItemStack getItemStack();

    /**
     * Changes the carried arrow and its potion effects. The count is normalized to one.
     *
     * @param item the arrow item
     * @throws IllegalArgumentException if the item is empty or is not an arrow
     */
    void setItemStack(ItemStack item);
}
