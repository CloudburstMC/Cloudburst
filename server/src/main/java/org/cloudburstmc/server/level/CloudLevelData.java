package org.cloudburstmc.server.level;

import lombok.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.gamerule.CloudGameRules;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Mutable state required to load, run, and save a level.
 *
 * <p>This is an internal server model. Storage providers may retain unrecognized
 * fields in {@link #getAdditionalData()} without adding format-specific fields
 * to this class.
 */
@Getter
@Setter
@ToString
public class CloudLevelData {
    private static final AtomicLongFieldUpdater<CloudLevelData> CURRENT_TICK_UPDATER =
            AtomicLongFieldUpdater.newUpdater(CloudLevelData.class, "currentTick");

    private final CloudGameRules gameRules;
    private final CloudEndFightData endFightData;
    private volatile long currentTick;
    @Getter(AccessLevel.NONE)
    private @Nullable Long seed;
    private int dimension;
    @Getter(AccessLevel.NONE)
    private @Nullable Identifier generator;
    private String generatorOptions = "";
    @Getter(AccessLevel.NONE)
    private @Nullable String name;
    private long time;
    private Vector3i spawn = Vector3i.from(0, 128, 0);
    private int serverChunkTickRange;
    private int rainTime;
    private int lightningTime;
    private float rainLevel;
    private float lightningLevel;
    private Difficulty difficulty = Difficulty.PEACEFUL;
    private GameMode gameMode = GameMode.SURVIVAL;
    private int netherScale;
    private long lastPlayed;
    private boolean hardcore;
    private Map<String, Object> additionalData = Map.of();

    public CloudLevelData() {
        this.gameRules = new CloudGameRules();
        this.endFightData = new CloudEndFightData();
    }

    private CloudLevelData(CloudLevelData source) {
        this.gameRules = new CloudGameRules(source.gameRules);
        this.endFightData = source.endFightData.copy();
        this.currentTick = source.currentTick;
        this.seed = source.seed;
        this.dimension = source.dimension;
        this.generator = source.generator;
        this.generatorOptions = source.generatorOptions;
        this.name = source.name;
        this.time = source.time;
        this.spawn = source.spawn;
        this.serverChunkTickRange = source.serverChunkTickRange;
        this.rainTime = source.rainTime;
        this.lightningTime = source.lightningTime;
        this.rainLevel = source.rainLevel;
        this.lightningLevel = source.lightningLevel;
        this.difficulty = source.difficulty;
        this.gameMode = source.gameMode;
        this.netherScale = source.netherScale;
        this.lastPlayed = source.lastPlayed;
        this.hardcore = source.hardcore;
        this.additionalData = source.additionalData;
    }

    /**
     * Creates level data initialized with server-wide defaults.
     *
     * @param defaults the server-wide default values
     * @return new mutable level data
     */
    public static CloudLevelData fromDefaults(CloudLevelDefaults defaults) {
        Objects.requireNonNull(defaults, "defaults");
        CloudLevelData data = new CloudLevelData();
        data.gameRules.loadFrom(defaults.getGameRules());
        data.difficulty = defaults.getDifficulty();
        return data;
    }

    /**
     * Creates an independent copy of this level state.
     *
     * @return copied level state
     */
    public CloudLevelData copy() {
        return new CloudLevelData(this);
    }

    public long getSeed() {
        return Objects.requireNonNull(this.seed, "seed has not been set");
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public Identifier getGenerator() {
        return Objects.requireNonNull(this.generator, "generator has not been set");
    }

    public String getName() {
        return Objects.requireNonNull(this.name, "level name has not been set");
    }

    public void setGeneratorOptions(@Nullable String generatorOptions) {
        this.generatorOptions = Objects.requireNonNullElse(generatorOptions, "");
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = Map.copyOf(Objects.requireNonNull(additionalData, "additionalData"));
    }

    public void tickTime() {
        if (this.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE)) {
            this.time++;
        }
    }

    public void tick() {
        CURRENT_TICK_UPDATER.incrementAndGet(this);
    }
}
