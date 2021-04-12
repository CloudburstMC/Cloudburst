package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

public interface CraftingGrid extends Inventory {

    Type getCraftingGridType();

    ItemStack getCraftingResult();

    @Override
    Player getHolder();

    void resetCraftingGrid();

    enum Type {
        CRAFTING_GRID_SMALL,
        CRAFTING_GRID_BIG
    }
}
