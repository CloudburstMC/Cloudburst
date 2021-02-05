package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Created by CreeperFace on 18.3.2017.
 */
public class PlayerMapInfoRequestEvent extends PlayerEvent implements Cancellable {

    private final ItemStack item;

    public PlayerMapInfoRequestEvent(Player player, ItemStack item) {
        super(player);
        this.item = item;
    }

    public ItemStack getMap() {
        return item;
    }
}
