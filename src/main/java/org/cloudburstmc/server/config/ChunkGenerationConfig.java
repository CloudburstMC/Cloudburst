package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkGenerationConfig {

    @Builder.Default
    private int queueSize = 8;

    @Builder.Default
    private int populationQueueSize = 8;

}
