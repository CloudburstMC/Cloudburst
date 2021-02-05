package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.api.player.Player;

import java.util.HashSet;
import java.util.Set;

public class PlayerChatEvent extends PlayerMessageEvent implements Cancellable {

    protected String format;

    protected Set<CommandSender> recipients = new HashSet<>();

    public PlayerChatEvent(Player player, String message) {
        this(player, message, "chat.type.text", null);
    }

    public PlayerChatEvent(Player player, String message, String format, Set<CommandSender> recipients) {
        super(player, message);

        this.format = format;

        if (recipients == null) {
            for (Permissible permissible : player.getServer().getPermissionManager().getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS)) {
                if (permissible instanceof CommandSender) {
                    this.recipients.add((CommandSender) permissible);
                }
            }

        } else {
            this.recipients = recipients;
        }
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
