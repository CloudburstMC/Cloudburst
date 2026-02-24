package org.cloudburstmc.api.inventory.view;

/**
 * Represents a 3×3 grid of 9 item slots (indices 0–8, row-major order:
 * slot 0 = top-left, slot 8 = bottom-right).
 *
 * <p>This interface is the common base for all container types that expose a
 * 3×3 grid to the player, regardless of how the block behaves mechanically.
 * Currently implemented by:</p>
 * <ul>
 *   <li>{@link DispenserView} — a dispenser block that fires or uses the dispensed item</li>
 *   <li>{@link DropperView} — a dropper block that always drops items as entities</li>
 *   <li>{@link CrafterView} — a crafter block that auto-crafts via redstone signal</li>
 * </ul>
 *
 * <p>Use this type when writing plugin code that treats any 3×3 storage block
 * uniformly (e.g. reading or randomising grid contents) without caring which
 * specific block type it is. To distinguish between the types at runtime, use
 * {@code instanceof DispenserView}, {@code instanceof DropperView}, or
 * {@code instanceof CrafterView}.</p>
 *
 * <p>Item slots are accessed via the inherited {@link SlotGroup#getItem(int)} and
 * {@link SlotGroup#setItem(int, org.cloudburstmc.api.item.ItemStack)} methods.</p>
 *
 * @see DispenserView
 * @see DropperView
 * @see CrafterView
 */
public interface GridView extends SlotGroup {
}
