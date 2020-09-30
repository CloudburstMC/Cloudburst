package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.player.Player;

public class PlayerJumpEvent extends PlayerEvent {

    public PlayerJumpEvent(Player player) {
        super(player);
    }
}
