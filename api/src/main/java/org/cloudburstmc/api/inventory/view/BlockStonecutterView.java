package org.cloudburstmc.api.inventory.view;

/**
 * A {@link StonecutterView} backed by a real stonecutter block in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} via
 * {@link BlockSlotGroup}.
 *
 * <p>Stonecutters have no associated block entity, so
 * {@link StatelessBlockSlotGroup#getBlockEntity()} always returns {@code null}.</p>
 *
 * @see StonecutterView
 * @see StatelessBlockSlotGroup
 */
public interface BlockStonecutterView extends StonecutterView, StatelessBlockSlotGroup {
}
