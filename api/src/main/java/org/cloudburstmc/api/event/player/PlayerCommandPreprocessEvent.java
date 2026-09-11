package org.cloudburstmc.api.event.player;

import lombok.Getter;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Fired before a command submitted by a player is dispatched.
 *
 * <p>Cancelling this event prevents execution. Command lines may include or omit a leading slash.</p>
 */
public final class PlayerCommandPreprocessEvent extends PlayerEvent implements Cancellable {

    /**
     * Command line that will be dispatched.
     */
    @Getter
    private String message;

    /**
     * Creates a command preprocessing event.
     *
     * @param player  the player submitting the command
     * @param message the command line
     */
    public PlayerCommandPreprocessEvent(Player player, String message) {
        super(player);
        this.setMessage(message);
    }

    /**
     * Replaces the command line that will be dispatched.
     *
     * @param message the replacement command line
     * @throws NullPointerException     if the message is {@code null}
     * @throws IllegalArgumentException if the message is blank
     */
    public void setMessage(String message) {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Player command must not be blank");
        }
        this.message = message;
    }
}
