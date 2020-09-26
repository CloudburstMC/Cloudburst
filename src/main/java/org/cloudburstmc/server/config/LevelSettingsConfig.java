package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LevelSettingsConfig {

    private boolean autoTickRate = true;

    private int autoTickRateLimit = 20;

    private boolean alwaysTickPlayers = false;

    private int baseTickRate = 1;

    private int chunkTimeoutAfterLoad = 30;

    private String defaultFormat = "minecraft:leveldb";

    private int chunkTimeoutAfterLastAccess = 120;

}
