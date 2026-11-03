package org.cloudburstmc.server.level.manager;

/**
 * Chunk preparation stages. Player-facing promotion follows initial lighting.
 */
public enum ChunkStage {
    /**
     * Storage data has been read or an empty chunk created.
     */
    LOADED,
    /**
     * Base terrain generation is complete.
     */
    GENERATED,
    /**
     * Feature population is complete.
     */
    POPULATED,
    /**
     * Terrain finishing is complete, without requiring initial lighting.
     */
    FINISHED,
    /**
     * Initial lighting is complete and the chunk may be promoted.
     */
    LIGHTED;

    /**
     * Returns the preparation stage required before this stage.
     *
     * @throws IllegalStateException for {@link #LOADED}, which has no preceding stage
     */
    public ChunkStage previous() {
        return switch (this) {
            case GENERATED -> LOADED;
            case POPULATED -> GENERATED;
            case FINISHED -> POPULATED;
            case LIGHTED -> FINISHED;
            case LOADED -> throw new IllegalStateException("Loaded stage has no dependency stage");
        };
    }
}
