package org.cloudburstmc.api.inventory.view;

/**
 * Represents the 3×3 item grid of a dropper container (9 slots, indices 0–8).
 *
 * <p>Use {@link SlotGroup#getItem(int)} and
 * {@link SlotGroup#setItem(int, org.cloudburstmc.api.item.ItemStack)}
 * with a slot index of 0–8 to access individual cells.</p>
 *
 * <p>Block-entity-backed droppers implement {@link BlockDropperView}.</p>
 *
 * @see BlockDropperView
 * @see DispenserView
 */
public interface DropperView extends GridView {
}
