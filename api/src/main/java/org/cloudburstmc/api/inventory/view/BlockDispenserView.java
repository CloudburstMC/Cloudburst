package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Dispenser;

/**
 * A {@link DispenserView} backed by a real dispenser block entity in the world.
 *
 * <p>Combines the 9-slot dispenser grid contract with {@link BlockSlotGroup} to expose
 * {@link BlockSlotGroup#getBlock()} and {@link BlockSlotGroup#getBlockEntity()}.</p>
 *
 * @see DispenserView
 * @see org.cloudburstmc.api.blockentity.Dispenser
 */
public interface BlockDispenserView extends DispenserView, BlockSlotGroup<Dispenser> {
}
