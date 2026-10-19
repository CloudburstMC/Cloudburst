package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * A persistent player-list entry identified by UUID rather than display name.
 */
public interface StoredUserEntry {

    UUID uuid();

    @Nullable String name();
}
