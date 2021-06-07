package org.cloudburstmc.server.inventory.transaction.action;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.CloudCraftingGrid;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@Log4j2
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
        ItemStack item = CloudItemRegistry.get().getCreativeItems().get(creativeItemNetId - 1);
        log.debug("Item pulled from creative items list: {}", item);
        item = item.withAmount(item.getBehavior().getMaxStackSize(item));
        log.debug("Setting creative output as: {}", item);
        return !item.isNull() && source.getCraftingInventory().setItem(CloudCraftingGrid.CRAFTING_RESULT_SLOT,
                item,
                false);
    }

    @Override
    public boolean execute(CloudPlayer source) {
        return true;
    }
}
