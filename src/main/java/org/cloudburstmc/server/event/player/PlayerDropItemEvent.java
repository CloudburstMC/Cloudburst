package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

public class PlayerDropItemEvent extends PlayerEvent implements Cancellable {

    private final Item drop;

    public PlayerDropItemEvent(Player player, Item drop) {
        super(player);
        this.drop = drop;
    }

    public Item getItem() {
        return this.drop;
    }
}
