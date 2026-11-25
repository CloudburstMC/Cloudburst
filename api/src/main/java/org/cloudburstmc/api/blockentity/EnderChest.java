package org.cloudburstmc.api.blockentity;

/**
 * A chest that opens the player's personal ender inventory.
 */
public interface EnderChest extends BlockEntity, Lidded {

    /**
     * Returns whether a block above the chest prevents it from opening.
     */
    boolean isBlocked();
}
