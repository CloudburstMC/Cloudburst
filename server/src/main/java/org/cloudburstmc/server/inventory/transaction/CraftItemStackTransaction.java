package org.cloudburstmc.server.inventory.transaction;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.CraftingGrid;
import org.cloudburstmc.server.crafting.CraftingRecipe;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;

public class CraftItemStackTransaction extends ItemStackTransaction {

    @Getter
    private final CraftingRecipe recipe;
    @Setter
    private CloudItemStack primaryOutput;
    @Getter
    private final List<CloudItemStack> extraOutputs = new ArrayList<>();

    public CraftItemStackTransaction(CloudPlayer source, CraftingRecipe recipe) {
        super(source);
        this.recipe = recipe;
    }

    public CraftingGrid.Type getCraftingGridType() {
        return getSource().getCraftingInventory().getCraftingGridType();
    }

    public int getCraftingGridSize() {
        return getCraftingGridType() == CraftingGrid.Type.CRAFTING_GRID_BIG ? 3 : 2;
    }

    public CraftingGrid getCraftingGrid() {
        return getSource().getCraftingInventory();
    }

    public void setExtraOutput(int slot, CloudItemStack item) {
        this.extraOutputs.add(slot, item);
    }

    public CloudItemStack getPrimaryOutput() {
        return primaryOutput == null ? (CloudItemStack) recipe.getResult() : primaryOutput;
    }
}
