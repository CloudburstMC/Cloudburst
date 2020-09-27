package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkTickingConfig {

    @Builder.Default
    private int tickRadius = 4;

    @Builder.Default
    private int perTick = 40;

    @Builder.Default
    private boolean clearTickList = true;

    @Builder.Default
    private boolean lightUpdates = false;

}
