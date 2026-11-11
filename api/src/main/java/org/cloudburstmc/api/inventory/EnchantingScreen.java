package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.view.EnchantingView;

/**
 * Represents an open enchanting table screen.
 */
public interface EnchantingScreen extends ContainerScreen {

    /**
     * @return the enchanting table associated with this screen
     */
    Block getBlock();

    /**
     * Returns the section giving access to the item-to-enchant and reagent slots.
     *
     * @return the enchanting table section
     */
    EnchantingView getEnchanting();
}
