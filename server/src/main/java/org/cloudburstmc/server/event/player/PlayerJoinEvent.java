package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.locale.TextContainer;
import org.cloudburstmc.server.player.Player;

public class PlayerJoinEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected TextContainer joinMessage;

    public PlayerJoinEvent(Player player, TextContainer joinMessage) {
        super(player);
        this.joinMessage = joinMessage;
    }

    public PlayerJoinEvent(Player player, String joinMessage) {
        super(player);
        this.joinMessage = new TextContainer(joinMessage);
    }

    public TextContainer getJoinMessage() {
        return joinMessage;
    }

    public void setJoinMessage(TextContainer joinMessage) {
        this.joinMessage = joinMessage;
    }

    public void setJoinMessage(String joinMessage) {
        this.setJoinMessage(new TextContainer(joinMessage));
    }
}
