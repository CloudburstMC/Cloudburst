package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLiquid;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

public class LiquidFlowEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final BlockState to;
    private final BlockBehaviorLiquid source;
    private final int newFlowDecay;

    public LiquidFlowEvent(BlockState to, BlockBehaviorLiquid source, int newFlowDecay) {
        super(to);
        this.to = to;
        this.source = source;
        this.newFlowDecay = newFlowDecay;
    }

    public int getNewFlowDecay() {
        return this.newFlowDecay;
    }

    public BlockBehaviorLiquid getSource() {
        return this.source;
    }

    public BlockState getTo() {
        return this.to;
    }
}
