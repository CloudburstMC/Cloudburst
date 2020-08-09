package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.player.Player;

public class PlayerJumpEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public PlayerJumpEvent(Player player) {
        super(player);
    }
}
