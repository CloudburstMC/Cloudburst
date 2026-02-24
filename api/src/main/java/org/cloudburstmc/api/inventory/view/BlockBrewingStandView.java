package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.BrewingStand;

/**
 * A {@link BrewingStandView} backed by a real brewing stand block entity in the world.
 * Adds access to the backing {@link org.cloudburstmc.api.block.Block} and
 * {@link BrewingStand} block entity via {@link BlockSlotGroup}.
 *
 * @see BrewingStandView
 * @see BlockSlotGroup
 */
public interface BlockBrewingStandView extends BrewingStandView, BlockSlotGroup<BrewingStand> {
}
