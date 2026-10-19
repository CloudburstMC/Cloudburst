package org.cloudburstmc.api.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.permission.ServerOperator;

import java.time.Instant;
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
     * Returns whether this player's UUID has an active ban.
     *
     * @return true if banned
     */
    boolean isBanned();

    /**
     * Adds or replaces this player's UUID ban without disconnecting an online player.
     *
     * @param reason the reason, or null for the default
     * @param expiration the expiration, or null for a permanent ban
     * @param source the issuer, or null for the default
     * @return the saved ban
     * @throws IllegalStateException if this player's UUID is unknown
     */
    BanEntry<UUID> ban(@Nullable String reason, @Nullable Instant expiration, @Nullable String source);

    /**
     * Returns whether this player's UUID is explicitly whitelisted.
     * This does not depend on whether the whitelist is enabled or the player is an operator.
     *
     * @return true if whitelisted
     */
    boolean isWhitelisted();

    /**
     * Sets whether this player's UUID is explicitly whitelisted.
     *
     * @param value true to add to the whitelist, false to remove
     * @throws IllegalStateException if this player's UUID is unknown
     */
    void setWhitelisted(boolean value);

    /**
     * Changes this player's operator status by UUID.
     *
     * @param value whether to grant operator status
     * @throws IllegalStateException if this player's UUID is unknown
     */
    @Override
    void setOp(boolean value);

    /**
     * Returns when this player first joined.
     *
     * @return milliseconds since the Unix epoch, or an empty optional if unknown
     */
    OptionalLong getFirstPlayed();

    /**
     * Returns when this player last joined.
     *
     * @return milliseconds since the Unix epoch, or an empty optional if unknown
     */
    OptionalLong getLastPlayed();

    /**
     * Returns whether stored data exists for this player.
     *
     * @return true if stored data exists
     */
    boolean hasPlayedBefore();
}
