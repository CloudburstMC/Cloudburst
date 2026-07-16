package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.event.Cancellable;

/**
 * Called before liquid at a position changes level or disappears.
 */
public final class LiquidLevelChangeEvent extends BlockEvent implements Cancellable {
    private final LiquidState currentLiquid;
    private LiquidState newLiquid;

    public LiquidLevelChangeEvent(Block block, LiquidState newLiquid) {
        super(block);
        this.currentLiquid = block.getLiquid();
        this.newLiquid = newLiquid;
    }

    /**
     * @return the liquid state before the change
     */
    public LiquidState getCurrentLiquid() {
        return this.currentLiquid;
    }

    /**
     * @return the new liquid state, or the empty state when the liquid will disappear
     */
    public LiquidState getNewLiquid() {
        return this.newLiquid;
    }

    /**
     * Sets the liquid state that will replace the current state. The empty state removes the liquid.
     *
     * @param newLiquid the replacement liquid state
     * @throws NullPointerException if {@code newLiquid} is {@code null}
     * @throws IllegalArgumentException if the replacement belongs to another liquid family
     */
    public void setNewLiquid(LiquidState newLiquid) {
        if (newLiquid == null) {
            throw new NullPointerException("newLiquid");
        }

        if (!newLiquid.isEmpty() && !this.currentLiquid.isSameFamily(newLiquid)) {
            throw new IllegalArgumentException("Cannot change liquid family");
        }

        this.newLiquid = newLiquid;
    }
}
