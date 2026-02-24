package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockHopperView;

/**
 * A hopper block entity with 5 slots that automatically pulls items from containers above and pushes items into containers below.
 */
public interface Hopper extends BlockEntity, BlockHopperView {
}
