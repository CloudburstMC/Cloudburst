package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.permission.Permissible;
import org.cloudburstmc.server.player.Player;

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
            for (Permissible permissible : CloudServer.getInstance().getPermissionManager().getPermissionSubscriptions(CloudServer.BROADCAST_CHANNEL_USERS)) {
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
