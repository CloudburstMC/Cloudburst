package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.view.InventoryView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudInventoryView extends CloudPlayerContainerView implements InventoryView {

    public CloudInventoryView(CloudPlayer holder, CloudContainer container) {
        super(ContainerViewTypes.INVENTORY, holder, container);
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

    @Override
    public int getHotbarSize() {
        return 9;
    }
}
