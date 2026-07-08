package org.cloudburstmc.server.level.manager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChunkTaskPriority {
    BLOCKING(0),
    HIGH(10),
    NORMAL(20),
    LOW(30);

    private final int sortOrder;
}
