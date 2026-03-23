package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;

/**
 * Called when a player's game mode is about to change.
 */
public final class PlayerGameModeChangeEvent extends PlayerEvent implements Cancellable {

    private final GameMode newGameMode;
    private final Cause cause;
    private boolean cancelled;
    @Nullable
    private Component cancelMessage;

    /**
     * @param player        the player whose game mode is changing
     * @param newGameMode   the game mode being applied
     * @param cause         the reason the game mode is changing
     * @param cancelMessage the message sent to the command sender if the event is
     *                      canceled, or {@code null} if no message should be sent.
     *                      Only meaningful when the cause is {@link Cause#COMMAND}.
     */
    public PlayerGameModeChangeEvent(Player player, GameMode newGameMode, Cause cause, @Nullable Component cancelMessage) {
        super(player);
        this.newGameMode = newGameMode;
        this.cause = cause;
        this.cancelMessage = cancelMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Returns the game mode the player is switching to.
     */
    public GameMode getNewGameMode() {
        return newGameMode;
    }

    /**
     * Returns the reason this game mode change was triggered.
     */
    public Cause getCause() {
        return cause;
    }

    /**
     * Returns the message that will be sent to the command sender if this event
     * is canceled. Only meaningful when the cause is {@link Cause#COMMAND}.
     *
     * @return the cancel message, or {@code null} if none is set
     */
    @Nullable
    public Component cancelMessage() {
        return cancelMessage;
    }

    /**
     * Sets the message that will be sent to the command sender if this event is
     * canceled.
     *
     * @param cancelMessage the message to send, or {@code null} to suppress it
     */
    public void cancelMessage(@Nullable Component cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    /**
     * The reason a player's game mode was changed.
     */
    public enum Cause {
        /**
         * A plugin changed the player's game mode via {@link Player#setGameMode(GameMode)}.
         */
        PLUGIN,
        /**
         * The {@code /gamemode} command was used.
         */
        COMMAND,
        /**
         * The player's game mode was changed because the server's default game mode
         * was applied (e.g. on join when {@code force-gamemode} is enabled).
         *
         * <p>The player may not be fully initialized when this cause is in use.
         * Check {@link Player#isSpawned()} before modifying player state.</p>
         */
        DEFAULT_GAMEMODE,
        /**
         * Fallback cause for use by third-party code constructing this event
         * without a specific reason.
         */
        UNKNOWN
    }
}
