package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.event.Cancellable;

/**
 * Called before flowing liquid destroys a block.
 */
public final class LiquidDestroyBlockEvent extends BlockEvent implements Cancellable {
    private final Block source;
    private final LiquidState liquid;

    public LiquidDestroyBlockEvent(Block source, Block target, LiquidState liquid) {
        super(target);
        this.source = source;
        this.liquid = liquid;
    }

    /**
     * @return the block from which the liquid is flowing
     */
    public Block getSource() {
        return this.source;
    }

    /**
     * @return the liquid that will destroy the block
     */
    public LiquidState getLiquid() {
        return this.liquid;
    }
}
