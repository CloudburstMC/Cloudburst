package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

public class PlayerGlassBottleFillEvent extends PlayerEvent implements Cancellable {

    protected final Item item;
    protected final BlockState target;

    public PlayerGlassBottleFillEvent(Player player, BlockState target, Item item) {
        super(player);
        this.target = target;
        this.item = item.clone();
    }

    public Item getItem() {
        return item;
    }

    public BlockState getBlock() {
        return target;
    }
}
