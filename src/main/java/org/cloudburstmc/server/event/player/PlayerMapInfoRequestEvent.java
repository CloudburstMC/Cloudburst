package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

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
