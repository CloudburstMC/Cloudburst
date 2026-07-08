package org.cloudburstmc.server.level.manager;

public enum ChunkStage {
    LOADED,
    GENERATED,
    POPULATED,
    FINISHED;

    public ChunkStage previous() {
        return switch (this) {
            case GENERATED -> LOADED;
            case POPULATED -> GENERATED;
            case FINISHED -> POPULATED;
            case LOADED -> throw new IllegalStateException("Loaded stage has no dependency stage");
        };
    }
}
