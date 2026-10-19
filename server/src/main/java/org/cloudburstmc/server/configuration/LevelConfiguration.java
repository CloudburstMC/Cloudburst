package org.cloudburstmc.server.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;
import org.cloudburstmc.server.configuration.validation.SettingConstraint;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Level-scoped settings resolved from shared defaults and a level's overrides.
 */
@Getter
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class LevelConfiguration {

    @Builder.Default
    @SettingConstraint(min = 1, max = 1)
    private int configVersion = ConfigurationVersions.CURRENT;

    @Builder.Default
    private Level level = new Level();

    @Builder.Default
    private ChunkTicking chunkTicking = new ChunkTicking();

    @Builder.Default
    private ChunkSending chunkSending = new ChunkSending();

    @Builder.Default
    private SpawnLimits spawnLimits = new SpawnLimits();

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class ChunkTicking {

        @Builder.Default
        @SettingConstraint(min = 0)
        private int tickRadius = 3;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int perTick = 40;

        @Builder.Default
        private boolean clearTickList = false;

        @Builder.Default
        private boolean lightUpdates = false;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class ChunkSending {

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxChunkRadius = 48;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxLoadedChunkRadius = 10;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int perTick = 4;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int spawnThreshold = 56;

        @Builder.Default
        private boolean cacheChunks = false;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class SpawnLimits {

        @Builder.Default
        @SettingConstraint(min = 0)
        private int monsters = 70;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int animals = 15;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int waterAnimals = 5;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int ambient = 15;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Level {

        // Maximum number of scheduled block ticks executed per game tick.
        // Capping this prevents TPS spikes during redstone storms while ensuring
        // delayed ticks still fire; they just carry over to the next tick.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxBlockTicks = 65536;

        // Maximum number of scheduled liquid ticks executed per game tick.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxLiquidTicks = 65536;

        // Tick delay for water when lava is horizontally adjacent.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int waterOverLavaFlowSpeed = 5;

        // Maximum number of chained neighbor-update callbacks that may fire
        // from a single block-change event before the chain is cut and a
        // warning is logged. Prevents infinite or runaway update loops from
        // overflowing the call stack. Set to -1 to disable the cap.
        @Builder.Default
        @SettingConstraint(min = -1)
        private int maxChainedNeighborUpdates = 1_000_000;
    }
}
