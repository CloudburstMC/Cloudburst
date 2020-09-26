package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkSendingConfig {

    private int maxChunkRadius = 10;

    private int perTick = 4;

    private int spawnThreshold = 56;

    private boolean cacheChunks = false;

}
