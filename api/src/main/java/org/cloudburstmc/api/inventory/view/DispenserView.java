package org.cloudburstmc.api.inventory.view;

/**
 * Represents the 3×3 item grid of a dispenser container (9 slots, indices 0–8).
 *
 * <p>Use {@link SlotGroup#getItem(int)} and {@link SlotGroup#setItem(int, org.cloudburstmc.api.item.ItemStack)}
 * with a slot index of 0–8 to access individual cells of the 3×3 grid
 * (row-major order: slot 0 = top-left, slot 8 = bottom-right).</p>
 *
 * <p>Block-entity-backed dispensers implement {@link BlockDispenserView}.
 * Droppers use the separate {@link DropperView} interface; they do <em>not</em> extend
 * {@code DispenserView}.</p>
 */
public interface DispenserView extends GridView {
}
