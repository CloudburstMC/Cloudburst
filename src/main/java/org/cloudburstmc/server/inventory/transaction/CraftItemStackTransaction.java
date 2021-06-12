package org.cloudburstmc.server.inventory.transaction;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.crafting.CraftingGrid;
import org.cloudburstmc.api.crafting.CraftingRecipe;
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

    public CraftingGrid getCraftingGrid() {
        return getSource().getCraftingInventory();
    }

    public void setExtraOutput(int slot, CloudItemStack item) {
        this.extraOutputs.add(slot, item);
    }

    public CloudItemStack getPrimaryOutput() {
        return primaryOutput == null ? (CloudItemStack) recipe.getResult() : primaryOutput;
    }

    @Override
    public boolean execute() {
        if (super.execute()) {
            getSource().getCraftingInventory().clearAll();
            this.sendInventories();
            return true;
        }
        return false;
    }
}
