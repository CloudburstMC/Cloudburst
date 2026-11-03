package org.cloudburstmc.server.level.manager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChunkTaskType {
    READ(0),
    GENERATE(0),
    POPULATE(1),
    FINISH(1),
    /**
     * Light work that does not reserve a chunk area.
     */
    LIGHT(-1);

    private final int exclusiveRadius;
}
