package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;

/**
 * Slots and transfer rules exposed to hoppers.
 */
public interface AutomatedBlockStorage extends BlockEntity, ItemContainer {

    default ItemContainer insertionSlots() {
        return this;
    }

    default boolean canInsertFrom(int slot, Direction side, ItemStack item) {
        return true;
    }

    default boolean canExtractBelow(int slot) {
        return true;
    }
}
