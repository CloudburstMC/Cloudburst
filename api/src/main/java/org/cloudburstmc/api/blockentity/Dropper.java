package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockDropperView;

/**
 * A dropper block entity with a 9-slot inventory that ejects items into the world or an adjacent container when powered by redstone.
 */
public interface Dropper extends BlockEntity, BlockDropperView {
}
