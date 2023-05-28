package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

public interface CraftingGrid extends Inventory {

    Type getCraftingGridType();

    ItemStack getCraftingResult();

    @Override
    Player getHolder();

    void resetCraftingGrid();

    default int getCraftingGridSize() {
        return getCraftingGridType() == Type.CRAFTING_GRID_SMALL ? 2 : 3;
    }

    enum Type {
        CRAFTING_GRID_SMALL,
        CRAFTING_GRID_BIG
    }
}
