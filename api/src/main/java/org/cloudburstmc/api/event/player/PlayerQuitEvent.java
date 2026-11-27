package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called on the server thread when a joined player disconnects.
 * Connections that never joined do not produce this event.
 * The quit message is broadcast after listeners return.
 */
public class PlayerQuitEvent extends PlayerEvent {

    private final QuitReason reason;
    private @Nullable Component quitMessage;
    private boolean autoSave = true;

    /**
     * @param player      the disconnecting player
     * @param quitMessage the message broadcast to other players, or {@code null} to omit it
     * @param reason      how the connection ended
     */
    public PlayerQuitEvent(Player player, @Nullable Component quitMessage, QuitReason reason) {
        super(player);
        this.quitMessage = quitMessage;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns the message broadcast to other players, or {@code null} when none is broadcast.
     */
    public @Nullable Component quitMessage() {
        return this.quitMessage;
    }

    /**
     * Sets the message broadcast to other players. Pass {@code null} to broadcast none.
     */
    public void quitMessage(@Nullable Component quitMessage) {
        this.quitMessage = quitMessage;
    }

    /**
     * Returns how the connection ended.
     */
    public QuitReason getReason() {
        return this.reason;
    }

    /**
     * Returns whether the player's data is saved after listeners return.
     */
    public boolean getAutoSave() {
        return this.autoSave;
    }

    /**
     * Sets whether the player's data is saved after listeners return.
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }
}
