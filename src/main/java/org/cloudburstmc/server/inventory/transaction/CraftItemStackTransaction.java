package org.cloudburstmc.server.inventory.transaction;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.crafting.CraftingGrid;
import org.cloudburstmc.api.crafting.CraftingRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;

public class CraftItemStackTransaction extends ItemStackTransaction {

    @Getter
    private final CraftingRecipe recipe;
    @Setter
    private ItemStack primaryOutput;
    @Getter
    private final List<ItemStack> extraOutputs = new ArrayList<>();

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

    public void setExtraOutput(int slot, ItemStack item) {
        this.extraOutputs.add(slot, item);
    }

    public ItemStack getPrimaryOutput() {
        return primaryOutput == null ? (ItemStack) recipe.getResult() : primaryOutput;
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
