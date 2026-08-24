package org.cloudburstmc.api.item.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Obtains the equipment slot an item occupies when used as equipment.
 */
@FunctionalInterface
public interface GetEquipmentSlotHandler {

    /**
     * Returns the item's equipment slot.
     *
     * @param item item being queried
     * @return equipment slot, or {@code null} when the item is not equipment
     */
    @Nullable
    EquipmentSlot execute(ItemStack item);
}
