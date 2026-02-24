package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockLecternView;

/**
 * Represents an open lectern screen.
 *
 * <p>A lectern is a book viewer, not a chest-like container — the player's own
 * inventory is <strong>not</strong> shown alongside it. This screen therefore extends
 * {@link InventoryScreen} directly rather than {@link ContainerScreen}.</p>
 */
public interface LecternScreen extends InventoryScreen {

    BlockLecternView getLectern();
}
