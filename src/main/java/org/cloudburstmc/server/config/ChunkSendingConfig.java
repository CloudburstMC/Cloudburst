package org.cloudburstmc.server.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class ChunkSendingConfig {

    int maxChunkRadius;

    int perTick;

    int spawnThreshold;

}
