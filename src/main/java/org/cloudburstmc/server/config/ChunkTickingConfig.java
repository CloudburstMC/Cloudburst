package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkTickingConfig {

    private int tickRadius = 4;

    private int perTick = 40;

    private boolean clearTickList = true;

    private boolean lightUpdates = false;

}
