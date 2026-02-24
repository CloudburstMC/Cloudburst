package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Slot group implementation for the full 36-slot player inventory.
 * Implements {@link org.cloudburstmc.api.inventory.view.PlayerInventoryView} and
 * {@link org.cloudburstmc.api.inventory.view.SelectedSlotAccess}.
 */
public class CloudPlayerInventory extends CloudPlayerContainerView implements PlayerInventoryView {

    public CloudPlayerInventory(CloudPlayer holder, CloudContainer container) {
        super(SlotGroupTypes.INVENTORY, holder, container);
    }

    @Override
    public ItemStack getSelectedItem() {
        return this.getItem(holder.getSelectedHotbarSlot());
    }

    @Override
    public void setSelectedItem(ItemStack item) {
        this.setItem(holder.getSelectedHotbarSlot(), item);
    }

    @Override
    public int getSelectedSlot() {
        return holder.getSelectedHotbarSlot();
    }

    @Override
    public void setSelectedSlot(int slot) {
        holder.setSelectedHotbarSlot(slot);
    }
}
