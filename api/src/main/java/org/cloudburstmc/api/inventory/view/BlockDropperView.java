package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Dropper;

/**
 * A {@link DropperView} backed by a real dropper block entity in the world.
 *
 * <p>Combines the 3×3 dropper storage contract with {@link BlockSlotGroup} to expose
 * {@link BlockSlotGroup#getBlock()} and {@link BlockSlotGroup#getBlockEntity()}.</p>
 *
 * @see DropperView
 * @see org.cloudburstmc.api.blockentity.Dropper
 */
public interface BlockDropperView extends DropperView, BlockSlotGroup<Dropper> {
}
