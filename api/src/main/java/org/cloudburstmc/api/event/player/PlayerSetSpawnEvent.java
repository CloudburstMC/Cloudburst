package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

/**
 * Fired whenever a player's personal respawn point changes: when they sleep in a bed,
 * right-click a charged respawn anchor, when a plugin or command sets their spawn, or when
 * the server clears it on respawn.
 *
 * <p>This event is {@link Cancellable}. Cancelling it prevents the spawn point from being
 * updated; the player will not receive a {@code SetSpawnPositionPacket}.</p>
 *
 * <p>Plugins may also redirect the spawn to a different location by calling
 * {@link #setNewSpawn(Location)}.</p>
 */
public final class PlayerSetSpawnEvent extends PlayerEvent implements Cancellable {

    private final Cause cause;
    @Nullable
    private final Location previousSpawn;

    @Nullable
    private Location newSpawn;
    private boolean forced;
    private boolean notifyPlayer;
    @Nullable
    private Component notification;

    public PlayerSetSpawnEvent(
            @NonNull Player player,
            @NonNull Cause cause,
            @Nullable Location previousSpawn,
            @Nullable Location newSpawn,
            boolean forced,
            boolean notifyPlayer,
            @Nullable Component notification
    ) {
        super(player);
        this.cause = cause;
        this.previousSpawn = previousSpawn;
        this.newSpawn = newSpawn;
        this.forced = forced;
        this.notifyPlayer = notifyPlayer;
        this.notification = notification;
    }

    /**
     * Why the spawn is being changed.
     */
    @NonNull
    public Cause getCause() {
        return cause;
    }

    /**
     * The player's spawn before this change, or {@code null} if they had none.
     */
    @Nullable
    public Location getPreviousSpawn() {
        return previousSpawn;
    }

    /**
     * The new spawn location the player will receive.
     * {@code null} means the personal spawn is being cleared and the player will
     * fall back to world spawn.
     */
    @Nullable
    public Location getNewSpawn() {
        return newSpawn;
    }

    /**
     * Override the new spawn location. Set to {@code null} to clear the personal spawn.
     */
    public void setNewSpawn(@Nullable Location newSpawn) {
        this.newSpawn = newSpawn;
    }

    /**
     * Returns whether this spawn change is forced. A forced spawn will override the player's
     * existing spawn even if they already have one set.
     */
    public boolean isForced() {
        return forced;
    }

    /**
     * Sets whether this spawn change is forced.
     */
    public void setForced(boolean forced) {
        this.forced = forced;
    }

    /**
     * Returns whether the player will be notified that their spawn point was changed.
     */
    public boolean willNotifyPlayer() {
        return notifyPlayer;
    }

    /**
     * Sets whether the player will be notified that their spawn point was changed.
     */
    public void setNotifyPlayer(boolean notifyPlayer) {
        this.notifyPlayer = notifyPlayer;
    }

    /**
     * Gets the notification message that will be sent to the player if
     * {@link #willNotifyPlayer()} returns {@code true}.
     *
     * @return the notification message, or {@code null} for no message
     */
    @Nullable
    public Component getNotification() {
        return notification;
    }

    /**
     * Sets the notification message to send to the player.
     *
     * @param notification the message, or {@code null} to suppress the notification
     */
    public void setNotification(@Nullable Component notification) {
        this.notification = notification;
    }

    public enum Cause {
        /**
         * Player slept in a bed.
         */
        BED,
        /**
         * Player interacted with a charged respawn anchor.
         */
        RESPAWN_ANCHOR,
        /**
         * The server is updating the player's tracked spawn as part of the respawn sequence.
         */
        PLAYER_RESPAWN,
        /**
         * {@code /spawnpoint} command or equivalent.
         */
        COMMAND,
        /**
         * Plugin directly called {@code setSpawn()}.
         */
        PLUGIN,
        /**
         * Spawn point is being cleared (set to null / world spawn).
         */
        RESET,
        /**
         * Cause could not be determined.
         */
        UNKNOWN
    }
}
