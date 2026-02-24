package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Crafter;

/**
 * A {@link CrafterView} backed by a real crafter block entity in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} and
 * {@link Crafter} block entity via {@link BlockSlotGroup}.
 *
 * @see CrafterView
 * @see BlockSlotGroup
 */
public interface BlockCrafterView extends CrafterView, BlockSlotGroup<Crafter> {
}
