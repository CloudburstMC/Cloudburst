package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.chunk.Chunk;

/**
 * Called on the server thread before a live chunk is unloaded. The chunk is
 * still accessible and may be modified during this event.
 *
 * <p>Unloading cannot be canceled. Listeners may control whether unsaved
 * changes are written to storage.
 */
public final class ChunkUnloadEvent extends ChunkEvent {

    private boolean saveChunk;

    /**
     * @param chunk     chunk being unloaded
     * @param saveChunk whether unsaved changes should be saved
     */
    public ChunkUnloadEvent(Chunk chunk, boolean saveChunk) {
        super(chunk);
        this.saveChunk = saveChunk;
    }

    /**
     * @return whether unsaved changes should be written before unloading
     */
    public boolean isSaveChunk() {
        return this.saveChunk;
    }

    /**
     * Controls whether unsaved changes are written before unloading.
     * Enabling saving does not force unchanged chunks to be saved.
     *
     * @param saveChunk whether unsaved changes should be saved
     */
    public void setSaveChunk(boolean saveChunk) {
        this.saveChunk = saveChunk;
    }
}
