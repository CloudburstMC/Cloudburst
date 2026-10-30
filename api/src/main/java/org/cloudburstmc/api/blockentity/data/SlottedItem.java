package org.cloudburstmc.api.blockentity.data;

import org.cloudburstmc.api.item.ItemStack;

/**
 * An item in a numbered inventory slot.
 * <p>
 * Its NBT form is produced by the server's serializer: one compound per slot holding
 * the item's fields plus a byte {@code Slot} tag.
 *
 * @param slot the inventory slot
 * @param item the item in the slot
 */
public record SlottedItem(
        int slot,
        ItemStack item) {
}
