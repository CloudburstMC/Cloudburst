package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.server.inventory.transaction.CraftItemStackTransaction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

public class CraftRecipeAction extends ItemStackAction {
    private final int recipeId;

    public CraftRecipeAction(int reqId, int recipeId) {
        super(reqId, null, null);
        this.recipeId = recipeId;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        return this.getTransaction() != null
                && this.getTransaction() instanceof CraftItemStackTransaction
                && recipeId == CloudRecipeRegistry.get().getRecipeNetId(((CraftItemStackTransaction) this.getTransaction()).getRecipe());
    }

    @Override
    public boolean execute(CloudPlayer player) {
        return isValid(player);
    }
}
