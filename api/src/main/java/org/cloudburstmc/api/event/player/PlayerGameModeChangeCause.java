package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;

/**
 * The reason a player's game mode was changed.
 */
public enum PlayerGameModeChangeCause {
    /**
     * A plugin changed the player's game mode via {@link Player#setGameMode(GameMode)}.
     */
    PLUGIN,
    /**
     * A command changed the player's game mode.
     */
    COMMAND,
    /**
     * The server's default game mode was applied, on join or when it changed while the game mode is forced.
     * The player may not have spawned yet, so check {@link Player#isSpawned()} before changing player state.
     */
    DEFAULT_GAMEMODE,
    /**
     * No more specific cause applies.
     */
    UNKNOWN
}
