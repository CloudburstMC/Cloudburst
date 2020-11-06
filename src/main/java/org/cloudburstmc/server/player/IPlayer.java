package org.cloudburstmc.server.player;

import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.metadata.Metadatable;
import org.cloudburstmc.server.permission.ServerOperator;

import java.util.OptionalLong;
import java.util.UUID;

/**
 * Represents the data for a player, whether they are online or offline.
 */
public interface IPlayer extends ServerOperator, Metadatable {

    /**
     * Checks if this player is currently online.
     *
     * @return true if they are online
     */
    boolean isOnline();

    /**
     * Returns the name of this player.
     *
     * @return Player name
     */
    String getName();

    UUID getServerId();

    /**
     * Checks if this player is banned.
     *
     * @return true if banned
     */
    boolean isBanned();

    /**
     * Sets this player to be banned or to be pardoned.
     *
     * @param value true if banned
     */
    void setBanned(boolean value);

    /**
     * Checks if this player is whitelisted.
     *
     * @return true if whitelisted
     */
    boolean isWhitelisted();

    /**
     * Adds or removes this player from the whitelist.
     *
     * @param value true if whitelisted
     */
    void setWhitelisted(boolean value);

    /**
     * Returns a {@link Player} object that this represents.
     *
     * If the player is online, this will return that player. Otherwise,
     * it will return null.
     *
     * @return Player
     */
    Player getPlayer();

    /**
     * Returns the {@link CloudServer} object carrying this player.
     *
     * @return The server instance
     */
    CloudServer getServer();

    /**
     * Returns the time this player first played on this server.
     *
     * If the player has never played before, this will return 0. Otherwise, it
     * will be the amount of milliseconds since midnight, January 1, 1970 UTC.
     *
     * @return An optional with the time this player first played on this server
     */
    OptionalLong getFirstPlayed();

    /**
     * Returns the time this player last joined in this server.
     *
     * If the player has never played before, this will return 0. Otherwise, it
     * will be the amount of milliseconds since midnight, January 1, 1970 UTC.
     *
     * @return An optional with the time this player last played on this server
     */
    OptionalLong getLastPlayed();

    /**
     * Checks if the player has played on this server before.
     *
     * @return true if this player has played before
     */
    boolean hasPlayedBefore();

}
