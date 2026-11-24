package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A trident in flight or lodged after impact, carrying the original item.
 */
public interface ThrownTrident extends AbstractArrow {

    /**
     * Returns the trident represented by this projectile.
     *
     * @return the trident item
     */
    ItemStack getTrident();

    /**
     * Changes the trident represented by this projectile.
     *
     * @param trident the trident item
     */
    void setTrident(ItemStack trident);
}
