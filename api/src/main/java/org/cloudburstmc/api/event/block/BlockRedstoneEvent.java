package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;

/**
 * Called when a block's redstone signal changes.
 */
public class BlockRedstoneEvent extends BlockEvent {

    private final int oldCurrent;
    private int newCurrent;

    public BlockRedstoneEvent(Block block, int oldCurrent, int newCurrent) {
        super(block);
        this.oldCurrent = oldCurrent;
        this.setNewCurrent(newCurrent);
    }

    /**
     * Returns the previous strength, from 0 to 15.
     */
    public int getOldCurrent() {
        return this.oldCurrent;
    }

    /**
     * Returns the proposed strength, from 0 to 15.
     */
    public int getNewCurrent() {
        return this.newCurrent;
    }

    /**
     * Sets the proposed strength. Signals determined by viewer count are reported but cannot be overridden.
     *
     * @param newCurrent a strength from 0 to 15
     */
    public void setNewCurrent(int newCurrent) {
        if (newCurrent < 0 || newCurrent > 15) {
            throw new IllegalArgumentException("Redstone current must be between 0 and 15");
        }

        this.newCurrent = newCurrent;
    }
}
