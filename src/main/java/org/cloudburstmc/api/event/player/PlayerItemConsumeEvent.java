package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

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
