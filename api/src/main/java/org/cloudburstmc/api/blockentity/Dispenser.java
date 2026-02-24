package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockDispenserView;

/**
 * A dispenser block entity with a 9-slot inventory that fires or uses items when powered by redstone.
 */
public interface Dispenser extends BlockEntity, BlockDispenserView {
}
