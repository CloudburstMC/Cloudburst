package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkGenerationConfig {

    private int queueSize = 8;

    private int populationQueueSize = 8;

}
