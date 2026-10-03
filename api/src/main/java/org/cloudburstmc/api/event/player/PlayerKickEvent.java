package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Fired before a player is kicked from the server. Cancelling aborts the kick.
 * The quit message is passed to {@link PlayerQuitEvent} for the leave broadcast.
 * It does not change the reason shown on the disconnect screen.
 */
public final class PlayerKickEvent extends PlayerEvent implements Cancellable {

    private final Reason reason;
    private final String reasonString;

    @Nullable
    private Component quitMessage;

    public PlayerKickEvent(Player player, Reason reason, @Nullable Component quitMessage) {
        this(player, reason, reason.toString(), quitMessage);
    }

    public PlayerKickEvent(Player player, Reason reason, String reasonString, @Nullable Component quitMessage) {
        super(player);
        this.quitMessage = quitMessage;
        this.reason = reason;
        this.reasonString = reasonString;
    }

    /**
     * Returns the reason string for this kick.
     *
     * @return the reason string
     */
    public String getReason() {
        return reasonString;
    }

    /**
     * Returns the kick reason enum.
     *
     * @return the reason
     */
    public Reason getReasonEnum() {
        return this.reason;
    }

    /**
     * Returns the quit message to pass to {@link PlayerQuitEvent}.
     *
     * @return the quit message component, or {@code null}
     */
    @Nullable
    public Component getQuitMessage() {
        return quitMessage;
    }

    /**
     * Sets the quit message to pass to {@link PlayerQuitEvent}.
     * Pass {@code null} to omit the initial broadcast message.
     *
     * @param quitMessage the new quit message, or {@code null}
     */
    public void setQuitMessage(@Nullable Component quitMessage) {
        this.quitMessage = quitMessage;
    }

    public enum Reason {
        NEW_CONNECTION,
        KICKED_BY_ADMIN,
        NOT_WHITELISTED,
        IP_BANNED,
        NAME_BANNED,
        INVALID_PVE,
        LOGIN_TIMEOUT,
        SERVER_FULL,
        FLYING_DISABLED,
        UNKNOWN;

        @Override
        public String toString() {
            return this.name();
        }
    }
}
