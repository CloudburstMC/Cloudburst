package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.locale.TextContainer;
import org.cloudburstmc.api.player.Player;

public final class PlayerLoginEvent extends PlayerEvent implements Cancellable {

    protected TextContainer kickMessage;


    public PlayerLoginEvent(Player player, TextContainer kickMessage) {
        super(player);
        this.kickMessage = kickMessage;
    }

    public PlayerLoginEvent(Player player, String kickMessage) {
        this(player, new TextContainer(kickMessage));
    }

    public String getKickMessage() {
        return kickMessage.getText();
    }

    public TextContainer getKickMessageContainer() {
        return kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = new TextContainer(kickMessage);
    }

    public void setKickMessage(TextContainer kickMessage) {
        this.kickMessage = kickMessage;
    }
}
