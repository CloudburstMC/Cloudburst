package org.cloudburstmc.server.inventory;

/**
 * @author CreeperFace
 */
public class BigCraftingGrid extends CraftingGrid {
    BigCraftingGrid(PlayerUIInventory playerUI) {
        super(playerUI, 32, 9);
    }
}
