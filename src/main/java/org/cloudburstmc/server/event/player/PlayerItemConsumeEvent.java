package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * Called when a player eats something
 */
public class PlayerItemConsumeEvent extends PlayerEvent implements Cancellable {

    private final ItemStack item;

    public PlayerItemConsumeEvent(Player player, ItemStack item) {
        super(player);
        this.item = item;
    }

    public ItemStack getItem() {
        return this.item;
    }
}
