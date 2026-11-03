package org.cloudburstmc.server;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.event.server.BroadcastMessageEvent;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks online recipients and delivers permission-filtered server broadcasts.
 */
public class ServerBroadcaster {

    private final Thread owner;
    private final EventManager events;
    private final Set<CommandSender> audience = new CopyOnWriteArraySet<>();

    /**
     * Creates a broadcaster with the console as its initial audience.
     * The owner identifies server-thread broadcasts for event dispatch.
     */
    public ServerBroadcaster(Thread owner, EventManager events, CommandSender console) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.events = Objects.requireNonNull(events, "events");
        this.audience.add(Objects.requireNonNull(console, "console"));
    }

    /**
     * Includes an online player in future broadcasts.
     */
    public void addPlayer(Player player) {
        this.audience.add(Objects.requireNonNull(player, "player"));
    }

    /**
     * Removes a player from future broadcasts.
     */
    public void removePlayer(Player player) {
        this.audience.remove(Objects.requireNonNull(player, "player"));
    }

    /**
     * Selects permitted recipients and delivers the message after broadcast listeners run.
     *
     * @return the final recipient count, or zero if canceled
     */
    public int broadcast(Component message, String permission) {
        Objects.requireNonNull(message, "message");
        String node = Permission.normalizeName(permission);
        Set<CommandSender> recipients = new LinkedHashSet<>();
        for (CommandSender recipient : this.audience) {
            if (recipient.hasPermission(node)) {
                recipients.add(recipient);
            }
        }

        BroadcastMessageEvent event = new BroadcastMessageEvent(Thread.currentThread() != this.owner, message, recipients);
        this.events.fire(event);
        if (event.isCancelled()) {
            return 0;
        }

        Component rendered = event.message();
        List<CommandSender> targets = List.copyOf(event.getRecipients());
        for (CommandSender recipient : targets) {
            recipient.sendMessage(rendered);
        }

        return targets.size();
    }
}
