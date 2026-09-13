package org.cloudburstmc.server.level;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Persisted state owned by the End dragon fight.
 */
@Getter
@Setter
@ToString
public final class CloudEndFightData {
    private boolean initialized;
    private boolean dragonKilled;
    private boolean dragonPreviouslyKilled;
    private int gatewayIndex;
    private @Nullable Integer exitPortalY;
    private @Nullable EndDragonRespawnStage respawnStage;
    private int respawnTime;

    public CloudEndFightData copy() {
        CloudEndFightData copy = new CloudEndFightData();
        copy.initialized = this.initialized;
        copy.dragonKilled = this.dragonKilled;
        copy.dragonPreviouslyKilled = this.dragonPreviouslyKilled;
        copy.gatewayIndex = this.gatewayIndex;
        copy.exitPortalY = this.exitPortalY;
        copy.respawnStage = this.respawnStage;
        copy.respawnTime = this.respawnTime;
        return copy;
    }
}
