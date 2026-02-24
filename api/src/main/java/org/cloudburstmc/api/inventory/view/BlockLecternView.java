package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Lectern;

/**
 * A {@link LecternView} backed by a real lectern block entity in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} and
 * {@link Lectern} block entity via {@link BlockSlotGroup}.
 *
 * @see LecternView
 * @see BlockSlotGroup
 */
public interface BlockLecternView extends LecternView, BlockSlotGroup<Lectern> {
}
