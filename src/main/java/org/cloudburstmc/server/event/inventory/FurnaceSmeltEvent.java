package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.blockentity.impl.FurnaceBlockEntity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.block.BlockEvent;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class FurnaceSmeltEvent extends BlockEvent implements Cancellable {

    private final FurnaceBlockEntity furnace;
    private final ItemStack source;
    private ItemStack result;

    public FurnaceSmeltEvent(FurnaceBlockEntity furnace, ItemStack source, ItemStack result) {
        super(furnace.getBlock());
        this.source = source.withAmount(1);
        this.result = result;
        this.furnace = furnace;
    }

    public FurnaceBlockEntity getFurnace() {
        return furnace;
    }

    public ItemStack getSource() {
        return source;
    }

    public ItemStack getResult() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = result;
    }
}