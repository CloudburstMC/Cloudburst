package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.Player;

/**
 * Called on the server thread when a joined player disconnects.
 * Connections that never joined do not produce this event.
 * The quit message is broadcast after listeners return.
 */
public class PlayerQuitEvent extends PlayerEvent {

    private final String reason;

    @Nullable
    private Component quitMessage;
    private boolean autoSave = true;

    public PlayerQuitEvent(Player player, @Nullable Component quitMessage, String reason) {
        this(player, quitMessage, true, reason);
    }

    public PlayerQuitEvent(Player player, @Nullable Component quitMessage) {
        this(player, quitMessage, true, "No reason");
    }

    public PlayerQuitEvent(Player player, @Nullable Component quitMessage, boolean autoSave, String reason) {
        super(player);
        this.quitMessage = quitMessage;
        this.autoSave = autoSave;
        this.reason = reason;
    }

    /**
     * Returns the quit message that will be broadcast, or {@code null} if suppressed.
     *
     * @return the quit message component, or {@code null}
     */
    @Nullable
    public Component getQuitMessage() {
        return quitMessage;
    }

    /**
     * Sets the quit message to broadcast. Pass {@code null} to suppress the message entirely.
     *
     * @param quitMessage the new quit message, or {@code null}
     */
    public void setQuitMessage(@Nullable Component quitMessage) {
        this.quitMessage = quitMessage;
    }

    public boolean getAutoSave() {
        return this.autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public String getReason() {
        return reason;
    }
}
