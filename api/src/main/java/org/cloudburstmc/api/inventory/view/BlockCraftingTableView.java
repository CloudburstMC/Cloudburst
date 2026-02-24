package org.cloudburstmc.api.inventory.view;

/**
 * A {@link CraftingTableView} backed by a real crafting table block in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} via
 * {@link BlockSlotGroup}.
 *
 * <p>Crafting tables have no associated block entity, so
 * {@link StatelessBlockSlotGroup#getBlockEntity()} always returns {@code null}.</p>
 *
 * @see CraftingTableView
 * @see StatelessBlockSlotGroup
 */
public interface BlockCraftingTableView extends CraftingTableView, StatelessBlockSlotGroup {
}
