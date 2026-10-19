package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.KickReason;
import org.cloudburstmc.api.player.Player;

/**
 * Fired before a player is kicked from the server. Cancelling aborts the kick.
 * The quit message is passed to {@link PlayerQuitEvent} for the leave broadcast.
 * It does not change the reason shown on the disconnect screen.
 */
public class PlayerKickEvent extends PlayerEvent implements Cancellable {

    private final KickReason reason;
    private final String reasonString;

    @Nullable
    private Component quitMessage;

    /**
     * Creates a kick event using the reason category's name as disconnect text.
     *
     * @param player the player being kicked
     * @param reason the kick category
     * @param quitMessage the initial leave message, or null to omit it
     */
    public PlayerKickEvent(Player player, KickReason reason, @Nullable Component quitMessage) {
        this(player, reason, reason.toString(), quitMessage);
    }

    /**
     * Creates a kick event with explicit disconnect text.
     *
     * @param player the player being kicked
     * @param reason the kick category
     * @param reasonString the disconnect text
     * @param quitMessage the initial leave message, or null to omit it
     */
    public PlayerKickEvent(Player player, KickReason reason, String reasonString, @Nullable Component quitMessage) {
        super(player);
        this.quitMessage = quitMessage;
        this.reason = reason;
        this.reasonString = reasonString;
    }

    /**
     * Returns the disconnect text supplied for this kick.
     *
     * @return the reason string
     */
    public String getReason() {
        return reasonString;
    }

    /**
     * Returns the category of this kick, independently of its disconnect text.
     *
     * @return the reason
     */
    public KickReason getKickReason() {
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
}
