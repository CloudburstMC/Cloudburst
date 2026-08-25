package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;

/**
 * An event involving experience produced by a block.
 */
public class BlockExpEvent extends BlockEvent {
    private int expToDrop;

    /**
     * Creates an event for experience produced by a block.
     *
     * @param block the block producing experience
     * @param expToDrop the experience to drop
     */
    public BlockExpEvent(Block block, int expToDrop) {
        super(block);
        this.expToDrop = expToDrop;
    }

    /**
     * Returns the experience that will be dropped after the event.
     *
     * @return the experience to drop
     */
    public int getExpToDrop() {
        return this.expToDrop;
    }

    /**
     * Sets the experience that will be dropped after the event.
     * Values less than one produce no experience.
     *
     * @param expToDrop the experience to drop
     */
    public void setExpToDrop(int expToDrop) {
        this.expToDrop = expToDrop;
    }
}
