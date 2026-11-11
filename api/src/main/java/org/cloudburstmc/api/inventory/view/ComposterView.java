package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.block.Block;

/**
 * A read-only view of the slot involved in an automated composter transfer.
 */
public interface ComposterView extends SlotGroup {

    /**
     * Returns the composter involved in the transfer.
     */
    Block getBlock();
}
