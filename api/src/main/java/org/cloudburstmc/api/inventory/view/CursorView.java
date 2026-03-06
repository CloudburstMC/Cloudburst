package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the player's cursor slot (the item currently held on the cursor
 * while dragging inside an open inventory screen.
 */
public interface CursorView extends PlayerSlotGroup {

    ItemStack getCursor();

    void setCursor(ItemStack itemStack);
}
