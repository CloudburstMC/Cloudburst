package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LevelSettingsConfig {

    @Builder.Default
    private boolean autoTickRate = true;

    @Builder.Default
    private int autoTickRateLimit = 20;

    @Builder.Default
    private boolean alwaysTickPlayers = false;

    @Builder.Default
    private int baseTickRate = 1;

    @Builder.Default
    private int chunkTimeoutAfterLoad = 30;

    @Builder.Default
    private String defaultFormat = "minecraft:leveldb";

    @Builder.Default
    private int chunkTimeoutAfterLastAccess = 120;

}
