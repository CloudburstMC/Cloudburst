package org.cloudburstmc.server.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.player.Player;

public class PlayerCommandPreprocessEvent extends PlayerMessageEvent implements Cancellable {

    public PlayerCommandPreprocessEvent(Player player, String message) {
        super(player, message);
    }
}
