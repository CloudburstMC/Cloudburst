package org.cloudburstmc.api.inventory.view;

/**
 * A {@link LoomView} backed by a real loom block in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} via
 * {@link StatelessBlockSlotGroup}.
 *
 * <p>Looms have no associated block entity, so
 * {@link StatelessBlockSlotGroup#getBlockEntity()} always returns {@code null}.</p>
 *
 * @see LoomView
 * @see StatelessBlockSlotGroup
 */
public interface BlockLoomView extends LoomView, StatelessBlockSlotGroup {
}
