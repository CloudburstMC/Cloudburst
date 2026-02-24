package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockFurnaceView;

/**
 * A furnace block entity with ingredient, fuel, and result slots that smelts items over time.
 */
public interface Furnace extends BlockEntity, BlockFurnaceView {
}
