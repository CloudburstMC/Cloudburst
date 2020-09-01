package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.player.Player;

/**
 * Called when a player eats something
 */
public class PlayerItemConsumeEvent extends PlayerEvent implements Cancellable {

    private final Item item;

    public PlayerItemConsumeEvent(Player player, Item item) {
        super(player);
        this.item = item;
    }

    public Item getItem() {
        return this.item.clone();
    }
}
