package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.item.ItemStack;

public interface HotbarView extends PlayerContainerView {

    ItemStack getSelectedItem();

    void setSelectedItem(ItemStack item);

    int getSelectedSlot();

    void setSelectedSlot(int slot);

    default int getHotbarSize() {
        return size();
    }
}
