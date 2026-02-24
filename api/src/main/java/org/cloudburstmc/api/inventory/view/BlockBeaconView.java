package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Beacon;

/**
 * A {@link BeaconView} backed by a real beacon block entity in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} and
 * {@link Beacon} block entity via {@link BlockSlotGroup}.
 *
 * @see BeaconView
 * @see BlockSlotGroup
 */
public interface BlockBeaconView extends BeaconView, BlockSlotGroup<Beacon> {
}
