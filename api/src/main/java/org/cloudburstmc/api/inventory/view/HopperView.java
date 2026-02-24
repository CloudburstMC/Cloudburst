package org.cloudburstmc.api.inventory.view;

/**
 * Represents the 5-slot row of a hopper container (5 slots, indices 0–4).
 *
 * <p>Use {@link SlotGroup#getItem(int)} and {@link SlotGroup#setItem(int, org.cloudburstmc.api.item.ItemStack)}
 * with a slot index of 0–4 to access the individual hopper slots
 * (left to right: slot 0 = leftmost, slot 4 = rightmost).</p>
 *
 * <p>Block-entity-backed hoppers implement {@link BlockHopperView}. Virtual hoppers
 * created by plugins via
 * {@link org.cloudburstmc.api.player.Player#createVirtualHopper(String)} implement only
 * this interface and have no backing block entity.</p>
 */
public interface HopperView extends SlotGroup {
}
