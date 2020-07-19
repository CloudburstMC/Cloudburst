package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

/**
 * Created by CreeperFace on 18.3.2017.
 */
public class PlayerMapInfoRequestEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Item item;

    public PlayerMapInfoRequestEvent(Player player, Item item) {
        super(player);
        this.item = item;
    }

    public Item getMap() {
        return item;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}
