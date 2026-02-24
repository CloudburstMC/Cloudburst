package org.cloudburstmc.api.inventory.view;

/**
 * A {@link SmithingView} backed by a real smithing table block in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} via
 * {@link BlockSlotGroup}.
 *
 * <p>Smithing tables have no associated block entity, so
 * {@link StatelessBlockSlotGroup#getBlockEntity()} always returns {@code null}.</p>
 *
 * @see SmithingView
 * @see StatelessBlockSlotGroup
 */
public interface BlockSmithingView extends SmithingView, StatelessBlockSlotGroup {
}
