package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Called early in the login sequence. Plugins may cancel this event to prevent
 * the player from completing login, optionally providing a kick message.
 */
public final class PlayerLoginEvent extends PlayerEvent implements Cancellable {

    private Component kickMessage;
    private boolean cancelled;

    public PlayerLoginEvent(Player player, Component kickMessage) {
        super(player);
        this.kickMessage = kickMessage;
    }

    public PlayerLoginEvent(Player player, String kickMessage) {
        this(player, Component.text(kickMessage));
    }

    public Component kickMessage() {
        return kickMessage;
    }

    public void kickMessage(Component kickMessage) {
        this.kickMessage = kickMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
