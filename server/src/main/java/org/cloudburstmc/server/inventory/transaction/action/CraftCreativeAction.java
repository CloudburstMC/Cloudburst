package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.server.inventory.CloudCraftingGrid;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class CraftCreativeAction extends ItemStackAction {
    private final int creativeItemNetId;

    public CraftCreativeAction(int reqId, int itemId) {
        super(reqId, null, null);
        this.creativeItemNetId = itemId;
    }

    @Override
    public boolean isValid(CloudPlayer source) {
        if (!source.isCreative()) {
            return false;
        }
        return source.getCraftingInventory().setItem(CloudCraftingGrid.CRAFTING_RESULT_SLOT,
                CloudItemRegistry.get().getCreativeItems().get(creativeItemNetId - 1),
                false);
    }

    @Override
    public boolean execute(CloudPlayer source) {
        return true;
    }
}
