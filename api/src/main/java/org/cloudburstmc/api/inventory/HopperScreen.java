package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockHopperView;

/**
 * Represents an open hopper screen.
 */
public interface HopperScreen extends ContainerScreen {

    /**
     * Returns the section containing the hopper's 5 item slots.
     *
     * <p>The returned view is always a {@link BlockHopperView} for real hopper block entities,
     * exposing {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlock()} and
     * {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlockEntity()}.</p>
     *
     * @return the hopper slot group
     */
    BlockHopperView getHopper();
}
