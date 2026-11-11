package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Furnace;

/**
 * A furnace view with access to its block and block entity.
 */
public interface BlockFurnaceView extends FurnaceView, BlockSlotGroup<Furnace> {
}
