package org.cloudburstmc.server.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class ChunkTickingConfig {

    int tickRadius;

    int perTick;

    boolean clearTickList;

}
