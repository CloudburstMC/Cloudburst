package org.cloudburstmc.server.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class LevelSettingsConfig {

    boolean autoTickRate;

    int autoTickRateLimit;

    boolean alwaysTickPlayers;

    int baseTickRate;

    int chunkTimeoutAfterLoad;

    String defaultFormat;

    int chunkTimeoutAfterLastAccess;

}
