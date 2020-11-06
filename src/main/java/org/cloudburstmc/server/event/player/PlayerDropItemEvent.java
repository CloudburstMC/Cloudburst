package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

public class PlayerDropItemEvent extends PlayerEvent implements Cancellable {

    private final ItemStack drop;

    public PlayerDropItemEvent(Player player, ItemStack drop) {
        super(player);
        this.drop = drop;
    }

    public ItemStack getItem() {
        return this.drop;
    }
}
