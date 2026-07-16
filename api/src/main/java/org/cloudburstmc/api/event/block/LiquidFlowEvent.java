package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.event.Cancellable;

/**
 * Called before liquid flows from one block position into another.
 */
public final class LiquidFlowEvent extends BlockEvent implements Cancellable {

    private final Block target;
    private final LiquidState liquid;

    public LiquidFlowEvent(Block source, Block target, LiquidState liquid) {
        super(source);
        this.target = target;
        this.liquid = liquid;
    }

    /**
     * @return the block the liquid will enter
     */
    public Block getTarget() {
        return this.target;
    }

    /**
     * @return the liquid state that will enter the target
     */
    public LiquidState getLiquid() {
        return this.liquid;
    }
}
