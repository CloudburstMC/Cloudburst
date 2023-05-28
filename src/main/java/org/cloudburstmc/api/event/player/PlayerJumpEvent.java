package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.player.Player;

public final class PlayerJumpEvent extends PlayerEvent {

    public PlayerJumpEvent(Player player) {
        super(player);
    }
}
