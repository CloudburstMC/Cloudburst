package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.view.BlockLecternView;

/**
 * A screen for reading the book on a lectern.
 */
public interface LecternScreen extends InventoryScreen {

    /**
     * Returns the lectern being read.
     */
    BlockLecternView getLectern();

    /**
     * Returns the block being read from.
     */
    default Block getBlock() {
        return this.getLectern().getBlock();
    }
}
