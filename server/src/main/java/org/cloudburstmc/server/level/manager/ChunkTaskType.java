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
     * Background light work. Reserves no chunk area, so it never blocks
     * generation on area conflicts.
     */
    LIGHT(-1);

    private final int exclusiveRadius;
}
