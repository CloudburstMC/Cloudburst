package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.view.HotbarView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudHotbarView extends CloudPlayerContainerView implements HotbarView {

    public CloudHotbarView(CloudPlayer player) {
        super(ContainerViewTypes.HOTBAR, player, player.getContainer(), 9, 0);
    }

    @Override
    public ItemStack getSelectedItem() {
        return null;
    }

    @Override
    public void setSelectedItem(ItemStack item) {

    }

    @Override
    public int getSelectedSlot() {
        return 0;
    }

    @Override
    public void setSelectedSlot(int slot) {

    }
}
