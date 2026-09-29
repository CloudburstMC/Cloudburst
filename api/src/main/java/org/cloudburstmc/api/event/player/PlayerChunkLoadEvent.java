package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.level.ChunkEvent;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called on the server thread after a chunk's terrain has been sent to a player.
 * This does not guarantee that the player has processed the terrain.
 *
 * <p>This event describes the player's view, not loading the chunk into its level.
 */
public final class PlayerChunkLoadEvent extends ChunkEvent {

    private final Player player;

    /**
     * @param chunk chunk sent to the player
     * @param player receiving player
     */
    public PlayerChunkLoadEvent(Chunk chunk, Player player) {
        super(chunk);
        this.player = Objects.requireNonNull(player, "player");
    }

    /**
     * @return player whose view now includes the chunk's terrain
     */
    public Player getPlayer() {
        return this.player;
    }
}
