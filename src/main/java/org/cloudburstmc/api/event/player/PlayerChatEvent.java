package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import java.util.Set;

public final class PlayerChatEvent extends PlayerMessageEvent implements Cancellable {

    protected String format;

    protected Set<CommandSender> recipients;

    public PlayerChatEvent(Player player, String message, String format, Set<CommandSender> recipients) {
        super(player, message);

        //TODO check for null
        this.format = format;
        this.recipients = recipients;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Set<CommandSender> getRecipients() {
        return this.recipients;
    }

    public void setRecipients(Set<CommandSender> recipients) {
        this.recipients = recipients;
    }
}
