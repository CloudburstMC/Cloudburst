package org.cloudburstmc.server.inventory.transaction.action;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.server.inventory.CloudCraftingGrid;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.cloudburstmc.api.item.ItemBehaviors.GET_MAX_STACK_SIZE;

@Log4j2
public class CraftCreativeAction extends ItemStackAction {

    @Inject
    ItemRegistry itemRegistry;

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

        ItemStack item = CloudItemRegistry.get().getCreativeItemByIndex(creativeItemNetId - 1);
        int maxStackSize = this.itemRegistry.getBehavior(item.getType(), GET_MAX_STACK_SIZE).execute();
        item = item.withCount(maxStackSize);
        return item != ItemStack.EMPTY &&
                source.getCraftingInventory().setItem(CloudCraftingGrid.CRAFTING_RESULT_SLOT, item, false);
    }

    @Override
    public boolean execute(CloudPlayer source) {
        return true;
    }

    @Override
    protected List<ItemStackResponseContainer> getContainers(CloudPlayer source) {
        return new ArrayList<>();
    }
}
