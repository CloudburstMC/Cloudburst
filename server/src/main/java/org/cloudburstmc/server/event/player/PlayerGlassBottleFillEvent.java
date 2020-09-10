package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

public class PlayerGlassBottleFillEvent extends PlayerEvent implements Cancellable {

    protected final ItemStack item;
    protected final BlockState target;

    public PlayerGlassBottleFillEvent(Player player, BlockState target, ItemStack item) {
        super(player);
        this.target = target;
        this.item = item.clone();
    }

    public ItemStack getItem() {
        return item;
    }

    public BlockState getBlock() {
        return target;
    }
}
