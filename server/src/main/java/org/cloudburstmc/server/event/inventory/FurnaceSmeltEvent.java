package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.blockentity.impl.FurnaceBlockEntity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.block.BlockEvent;
import org.cloudburstmc.server.item.behavior.Item;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class FurnaceSmeltEvent extends BlockEvent implements Cancellable {

    private final FurnaceBlockEntity furnace;
    private final Item source;
    private Item result;

    public FurnaceSmeltEvent(FurnaceBlockEntity furnace, Item source, Item result) {
        super(furnace.getBlock());
        this.source = source.clone();
        this.source.setCount(1);
        this.result = result;
        this.furnace = furnace;
    }

    public FurnaceBlockEntity getFurnace() {
        return furnace;
    }

    public Item getSource() {
        return source;
    }

    public Item getResult() {
        return result;
    }

    public void setResult(Item result) {
        this.result = result;
    }
}