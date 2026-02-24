package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.HotbarView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Slot group implementation for the 9-slot hotbar (slots 0–8 of the player inventory container).
 * Implements {@link org.cloudburstmc.api.inventory.view.SelectedSlotAccess} for selected-item tracking.
 */
public class CloudHotbarView extends CloudPlayerContainerView implements HotbarView {

    public CloudHotbarView(CloudPlayer player) {
        super(SlotGroupTypes.HOTBAR, player, player.getContainer(), 9, 0);
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
