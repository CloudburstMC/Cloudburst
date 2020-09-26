package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NetworkConfig {

    private int compressionLevel = 7;

    private boolean asyncCompression = true;

    private int batchThreshold = 256;

}
