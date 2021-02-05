package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.player.Player;

public class PlayerLoginEvent extends PlayerEvent implements Cancellable {

    protected String kickMessage;


    public PlayerLoginEvent(Player player, String kickMessage) {
        super(player);
        this.kickMessage = kickMessage;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }
}
