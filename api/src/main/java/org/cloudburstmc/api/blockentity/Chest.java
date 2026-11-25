package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.ItemContainer;

/**
 * A chest or one half of a double chest.
 */
public interface Chest extends BlockEntity, Lidded {

    /**
     * Returns the inventory of this block, excluding any paired half.
     */
    ItemContainer getBlockInventory();

    /**
     * Returns the joined inventory when both halves are loaded, or this block's inventory otherwise.
     * A joined inventory cannot be modified after the pair separates.
     */
    ItemContainer getInventory();

    /**
     * Returns the adjacent half of a double chest, or {@code null} if unavailable.
     */
    @Nullable
    Chest getPair();

    /**
     * Returns whether an obstruction prevents either half from opening.
     */
    boolean isBlocked();
}
