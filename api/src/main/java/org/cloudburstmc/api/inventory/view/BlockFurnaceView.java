package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Furnace;

/**
 * A {@link FurnaceView} backed by a real smelting block entity
 * (furnace, blast furnace, or smoker) in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} and
 * {@link Furnace} block entity via {@link BlockSlotGroup}.
 *
 * @see FurnaceView
 * @see BlockSlotGroup
 */
public interface BlockFurnaceView extends FurnaceView, BlockSlotGroup<Furnace> {
}
