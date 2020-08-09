package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.player.PlayerLoginData;

/**
 * Called when the player logs in, before things have been set up
 */
public class PlayerPreLoginEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected PlayerLoginData loginData;
    protected String kickMessage;

    public PlayerPreLoginEvent(PlayerLoginData loginData, String kickMessage) {
        this.loginData = loginData;
        this.kickMessage = kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }

    public String getKickMessage() {
        return this.kickMessage;
    }

    public PlayerLoginData getLoginData() {
        return loginData;
    }
}
