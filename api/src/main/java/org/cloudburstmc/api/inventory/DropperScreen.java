package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockDropperView;

/**
 * Represents an open dropper screen.
 *
 * <p>Although a dropper and dispenser share the same 3×3 slot layout, they are
 * distinct block types with different behaviour. This interface is therefore
 * separate from {@link DispenserScreen} rather than extending it.</p>
 *
 * @see DispenserScreen
 */
public interface DropperScreen extends ContainerScreen {

    /**
     * Returns the 3×3 item grid section of this container.
     *
     * @return the dropper slot group
     */
    BlockDropperView getDropper();
}
