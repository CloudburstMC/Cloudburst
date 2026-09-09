package org.cloudburstmc.api.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.skin.Skin;

import java.util.UUID;

/**
 * The identity and skin presented by a player connection.
 */
public interface PlayerProfile {

    /**
     * Returns the display name presented by the player profile.
     *
     * @return the player name
     */
    String getName();

    /**
     * Returns the stable identifier used by this server for the player.
     *
     * @return the player identifier
     */
    UUID getUniqueId();

    /**
     * Returns the player's Minecraft services identifier when supplied by authentication.
     *
     * @return the Minecraft services identifier, or {@code null} when unavailable
     */
    @Nullable
    String getMinecraftId();

    /**
     * Returns the Xbox user identifier when the profile was authenticated by Xbox Live.
     *
     * @return the Xbox user identifier, or {@code null} for an unauthenticated profile
     */
    @Nullable
    String getXuid();

    /**
     * Returns whether the identity was authenticated by Xbox Live.
     *
     * @return {@code true} when authenticated
     */
    boolean isAuthenticated();

    /**
     * Returns the skin associated with this profile.
     *
     * @return the profile skin
     */
    Skin getSkin();
}
