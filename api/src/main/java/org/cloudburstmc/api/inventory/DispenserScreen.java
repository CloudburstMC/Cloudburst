package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockDispenserView;

/**
 * Represents an open dispenser screen.
 */
public interface DispenserScreen extends ContainerScreen {

    /**
     * Returns the 3×3 item grid section of this container.
     *
     * <p>The returned view is always a {@link BlockDispenserView} for real dispenser block
     * entities, exposing {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlock()}
     * and {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlockEntity()}.</p>
     *
     * @return the dispenser slot group
     */
    BlockDispenserView getDispenser();
}
