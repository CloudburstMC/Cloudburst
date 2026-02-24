package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Hopper;

/**
 * A {@link HopperView} backed by a real hopper block entity in the world.
 *
 * <p>Combines the 5-slot hopper storage contract with {@link BlockSlotGroup} to expose
 * {@link BlockSlotGroup#getBlock()} and {@link BlockSlotGroup#getBlockEntity()}.</p>
 *
 * @see HopperView
 * @see org.cloudburstmc.api.blockentity.Hopper
 */
public interface BlockHopperView extends HopperView, BlockSlotGroup<Hopper> {
}
