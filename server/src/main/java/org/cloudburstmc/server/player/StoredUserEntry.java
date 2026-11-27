package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * A persistent player-list entry. An entry without a UUID reserves a name for a player who has not
 * signed in yet, and gains the player's UUID when that player first signs in.
 */
public interface StoredUserEntry {

    @Nullable
    UUID uuid();

    @Nullable
    String name();
}
