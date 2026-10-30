package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.item.ItemStack;

/**
 * An item in a numbered inventory slot.
 *
 * @param slot the inventory slot
 * @param item the item in the slot
 */
public record SlottedItem(
        @JsonProperty("Slot") int slot,
        @JsonProperty("Item") ItemStack item) {
}
