package org.cloudburstmc.server.level;

import lombok.Getter;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.server.level.gamerule.CloudGameRules;

import java.util.Objects;

/**
 * Server-wide values copied into newly loaded levels before provider data is applied.
 */
@Getter
public class CloudLevelDefaults {
    private final CloudGameRules gameRules = new CloudGameRules();
    private Difficulty difficulty = Difficulty.PEACEFUL;

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    }
}
