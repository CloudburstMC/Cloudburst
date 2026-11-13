package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.inventory.ItemContainer;

/**
 * A typed section of an inventory, such as a player's armor slots or a furnace's slots.
 */
public interface SlotGroup extends ItemContainer {

    /**
     * Identifies the kind of slots in this group.
     *
     * @return the slot group type
     */
    SlotGroupType<? extends SlotGroup> getSlotGroupType();
}
