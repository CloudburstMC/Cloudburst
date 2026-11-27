package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Fired before a player is kicked from the server. Cancelling aborts the kick.
 */
public class PlayerKickEvent extends PlayerEvent implements Cancellable {

    private final KickCause cause;
    private Component reason;
    private @Nullable Component leaveMessage;

    /**
     * @param player       the player being kicked
     * @param reason       the message shown to the kicked player
     * @param leaveMessage the message broadcast to other players, or {@code null} to omit it
     * @param cause        why the player is kicked
     */
    public PlayerKickEvent(Player player, Component reason, @Nullable Component leaveMessage, KickCause cause) {
        super(player);
        this.reason = Objects.requireNonNull(reason, "reason");
        this.leaveMessage = leaveMessage;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /**
     * Returns the message shown to the kicked player.
     */
    public Component reason() {
        return this.reason;
    }

    /**
     * Sets the message shown to the kicked player.
     */
    public void reason(Component reason) {
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns the message broadcast to other players, or {@code null} when none is broadcast.
     */
    public @Nullable Component leaveMessage() {
        return this.leaveMessage;
    }

    /**
     * Sets the message broadcast to other players. Pass {@code null} to broadcast none.
     */
    public void leaveMessage(@Nullable Component leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    /**
     * Returns why the player is kicked.
     */
    public KickCause getCause() {
        return this.cause;
    }
}
