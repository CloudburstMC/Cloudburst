package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

public class PlayerGlassBottleFillEvent extends PlayerEvent implements Cancellable {

    protected final ItemStack item;
    protected final BlockState target;

    public PlayerGlassBottleFillEvent(Player player, BlockState target, ItemStack item) {
        super(player);
        this.target = target;
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public BlockState getBlock() {
        return target;
    }
}
