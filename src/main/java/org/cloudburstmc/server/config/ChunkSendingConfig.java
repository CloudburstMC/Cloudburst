package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkSendingConfig {

    @Builder.Default
    private int maxChunkRadius = 10;

    @Builder.Default
    private int perTick = 4;

    @Builder.Default
    private int spawnThreshold = 56;

    @Builder.Default
    private boolean cacheChunks = false;

}
