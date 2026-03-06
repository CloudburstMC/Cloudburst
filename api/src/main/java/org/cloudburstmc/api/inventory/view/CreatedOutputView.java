package org.cloudburstmc.api.inventory.view;

/**
 * Represents the output slot of a crafting result (the slot that shows the
 * crafted item before the player picks it up.
 *
 * <p>This is a player-bound slot group. Use {@link SlotGroup#getItem(int)} with slot {@code 0}
 * to read the crafted item, or {@link SlotGroup#setItem(int, org.cloudburstmc.api.item.ItemStack)}
 * to override it.</p>
 */
public interface CreatedOutputView extends PlayerSlotGroup {
}
