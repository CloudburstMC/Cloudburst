package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.chunk.Chunk;

import java.util.Objects;

/**
 * Associates an event with a chunk and its owning level. For player view events,
 * this level may differ from the player's level during a level change.
 */
public abstract class ChunkEvent extends LevelEvent {

    private final Chunk chunk;

    /**
     * @param chunk affected chunk
     */
    protected ChunkEvent(Chunk chunk) {
        super(Objects.requireNonNull(chunk, "chunk").getLevel());
        this.chunk = chunk;
    }

    /**
     * @return affected chunk
     */
    public Chunk getChunk() {
        return chunk;
    }
}
