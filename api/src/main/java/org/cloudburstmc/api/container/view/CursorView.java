package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.item.ItemStack;

public interface CursorView extends PlayerContainerView {

    ItemStack getCursor();

    void setCursor(ItemStack itemStack);
}
