package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.locale.TextContainer;
import org.cloudburstmc.api.player.Player;

public class PlayerJoinEvent extends PlayerEvent {

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
