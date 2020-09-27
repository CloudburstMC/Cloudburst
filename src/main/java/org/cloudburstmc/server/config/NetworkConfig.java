package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NetworkConfig {

    @Builder.Default
    private int compressionLevel = 7;

    @Builder.Default
    private boolean asyncCompression = true;

    @Builder.Default
    private int batchThreshold = 256;

}
