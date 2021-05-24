package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.crafting.CraftingRecipe;
import org.cloudburstmc.server.inventory.CloudCraftingGrid;
import org.cloudburstmc.server.inventory.transaction.CraftItemStackTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

public class CraftRecipeAction extends ItemStackAction {
    private final int recipeId;

    public CraftRecipeAction(int reqId, int recipeId) {
        super(reqId, null, null);
        this.recipeId = recipeId;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        if (this.getTransaction() == null || !(this.getTransaction() instanceof CraftItemStackTransaction))
            return false;
        CraftItemStackTransaction transaction = ((CraftItemStackTransaction) getTransaction());
        CraftingRecipe recipe = transaction.getRecipe();
        if (CloudRecipeRegistry.get().getRecipeNetId(recipe.getId()) != this.recipeId) return false;

        int size = transaction.getCraftingGridSize();

        ItemStack[][] inputs = new ItemStack[size][size];
        ItemStack[][] extraOutputs = new ItemStack[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int slot = (size * r) + c;
                if (transaction.getExtraOutputs().size() <= slot || transaction.getExtraOutputs().get(slot) == null)
                    transaction.getExtraOutputs().add(slot, CloudItemRegistry.get().AIR);
                extraOutputs[r][c] = transaction.getExtraOutputs().get(slot);
                inputs[r][c] = transaction.getCraftingGrid().getItem(slot);
            }
        }

        return recipe.matchItems(inputs, extraOutputs);
    }

    @Override
    public boolean execute(CloudPlayer player) {
        CraftItemStackTransaction transaction = (CraftItemStackTransaction) getTransaction();
        return transaction.getCraftingGrid().setItem(CloudCraftingGrid.CRAFTING_RESULT_SLOT, transaction.getPrimaryOutput(), false);
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        super.onAddToTransaction(transaction);
        transaction.addInventory(transaction.getSource().getInventoryManager().getCraftingGrid());
    }
}
