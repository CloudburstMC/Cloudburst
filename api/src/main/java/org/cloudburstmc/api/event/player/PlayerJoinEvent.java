package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.Player;

/**
 * Fired when a player joins the server. The join message is broadcast to all online players
 * after the event is processed; set it to {@code null} to suppress the broadcast entirely.
 */
public final class PlayerJoinEvent extends PlayerEvent {

    @Nullable
    private Component joinMessage;

    public PlayerJoinEvent(Player player, @Nullable Component joinMessage) {
        super(player);
        this.joinMessage = joinMessage;
    }

    /**
     * Returns the join message that will be broadcast, or {@code null} if suppressed.
     *
     * @return the join message component, or {@code null}
     */
    @Nullable
    public Component getJoinMessage() {
        return joinMessage;
    }

    /**
     * Sets the join message to broadcast. Pass {@code null} to suppress the message entirely.
     *
     * @param joinMessage the new join message, or {@code null}
     */
    public void setJoinMessage(@Nullable Component joinMessage) {
        this.joinMessage = joinMessage;
    }
}
