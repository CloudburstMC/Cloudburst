package org.cloudburstmc.api.inventory.view;

/**
 * A {@link CartographyView} backed by a real cartography table block in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} via
 * {@link BlockSlotGroup}.
 *
 * <p>Cartography tables have no associated block entity, so
 * {@link StatelessBlockSlotGroup#getBlockEntity()} always returns {@code null}.</p>
 *
 * @see CartographyView
 * @see StatelessBlockSlotGroup
 */
public interface BlockCartographyView extends CartographyView, StatelessBlockSlotGroup {
}
