package org.cloudburstmc.api.item.data;

import org.cloudburstmc.api.item.ItemStack;

import java.util.Map;
import java.util.Objects;

/**
 * The occupied slots of a portable shulker box.
 *
 * @param items the items by zero-based slot, copied when this value is created
 */
public record ShulkerBoxContents(Map<Integer, ItemStack> items) {

    public static final int SLOT_COUNT = 27;

    public ShulkerBoxContents {
        items = Map.copyOf(items);
        items.forEach((slot, item) -> {
            Objects.checkIndex(slot, SLOT_COUNT);
            if (item.isEmpty()) {
                throw new IllegalArgumentException("Contents must only contain occupied slots");
            }
        });
    }
}
