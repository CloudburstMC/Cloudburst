package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.block.BlockEvent;
import org.cloudburstmc.api.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class FurnaceSmeltEvent extends BlockEvent implements Cancellable {

    private final Furnace furnace;
    private final ItemStack source;
    private ItemStack result;

    public FurnaceSmeltEvent(Furnace furnace, ItemStack source, ItemStack result) {
        super(furnace.getBlock());
        this.source = source.withAmount(1);
        this.result = result;
        this.furnace = furnace;
    }

    public Furnace getFurnace() {
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