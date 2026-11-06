package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.inventory.ItemContainer;

/**
 * Represents a named group of item slots within an open inventory screen.
 *
 * <p>A slot group models one logical section of a screen, such as the furnace
 * ingredient slot, the player's 36-slot main inventory, or an armor equipment
 * section. Multiple slot groups are combined inside a single
 * {@link org.cloudburstmc.api.inventory.InventoryScreen} to form the complete window shown
 * to the player.</p>
 *
 * <p>Typed sub-interfaces such as {@link FurnaceView}, {@link StorageView}, and {@link ArmorView}
 * add named accessors for the slots specific to each container type. Most plugin code should
 * interact with those typed interfaces rather than this base.</p>
 */
public interface SlotGroup extends ItemContainer {

    /**
     * Returns the type token identifying what kind of slot group this is.
     *
     * @return the slot group type
     */
    SlotGroupType<? extends SlotGroup> getSlotGroupType();
}
