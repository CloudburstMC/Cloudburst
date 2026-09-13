package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;

/**
 * A thrown trident projectile.
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
