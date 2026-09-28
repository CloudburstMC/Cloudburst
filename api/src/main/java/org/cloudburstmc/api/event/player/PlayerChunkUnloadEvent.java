package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.level.ChunkEvent;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called on the server thread when previously sent terrain leaves a player's
 * view, including level changes and disconnects.
 *
 * <p>This does not unload the chunk from its level.
 */
public final class PlayerChunkUnloadEvent extends ChunkEvent {

    private final Player player;

    /**
     * @param chunk  chunk removed from the player's view
     * @param player affected player
     */
    public PlayerChunkUnloadEvent(Chunk chunk, Player player) {
        super(chunk);
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return player whose view no longer includes the chunk's terrain
     */
    public Player getPlayer() {
        return this.player;
    }
}
