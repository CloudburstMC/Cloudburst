package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Fired before a player is kicked from the server. The event is cancellable; cancelling it
 * aborts the kick. The {@code quitMessage} is the disconnect screen text shown to the player
 * and, if non-null, broadcast to all online players.
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
     * Returns the internal reason string for this kick.
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
     * Returns the disconnect message shown to the player (and broadcast to others),
     * or {@code null} if suppressed.
     *
     * @return the quit message component, or {@code null}
     */
    @Nullable
    public Component getQuitMessage() {
        return quitMessage;
    }

    /**
     * Sets the quit/disconnect message. Pass {@code null} to suppress it.
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
