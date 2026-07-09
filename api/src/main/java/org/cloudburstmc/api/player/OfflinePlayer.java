package org.cloudburstmc.api.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.permission.ServerOperator;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Represents a player identity and stored player state that can be queried while the player is not connected.
 */
public interface OfflinePlayer extends ServerOperator {

    /**
     * Returns the server associated with this player.
     *
     * @return the server
     */
    Server getServer();

    /**
     * Returns the last known player name.
     *
     * @return the last known name, or {@code null} if no name is known
     */
    @Nullable
    String getName();

    /**
     * Returns the player UUID.
     *
     * @return the UUID, or {@code null} if no UUID is known
     */
    @Nullable
    UUID getUniqueId();

    /**
     * Returns whether a matching player is currently online.
     *
     * @return true if the player is online
     */
    boolean isOnline();

    /**
     * Returns whether the matching online player connection is still active.
     *
     * @return true if the current online player connection is active
     */
    boolean isConnected();

    /**
     * Returns the matching online player.
     *
     * @return the online player, or an empty optional if the player is not online
     */
    Optional<Player> getPlayer();

    /**
     * Returns whether this player is banned by name.
     *
     * @return true if banned
     */
    boolean isBanned();

    /**
     * Sets whether this player is banned by name.
     *
     * @param value true to ban, false to pardon
     */
    void setBanned(boolean value);

    /**
     * Returns whether this player is whitelisted.
     *
     * @return true if whitelisted
     */
    boolean isWhitelisted();

    /**
     * Sets whether this player is whitelisted.
     *
     * @param value true to add to the whitelist, false to remove
     */
    void setWhitelisted(boolean value);

    /**
     * Returns when this player first joined.
     *
     * @return the first join time, or an empty optional if unknown
     */
    OptionalLong getFirstPlayed();

    /**
     * Returns when this player last joined.
     *
     * @return the last join time, or an empty optional if unknown
     */
    OptionalLong getLastPlayed();

    /**
     * Returns whether stored data exists for this player.
     *
     * @return true if stored data exists
     */
    boolean hasPlayedBefore();
}
