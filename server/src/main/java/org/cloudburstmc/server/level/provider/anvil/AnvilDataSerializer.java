package org.cloudburstmc.server.level.provider.anvil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.CloudEndFightData;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.EndDragonRespawnStage;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnvilDataSerializer {
    public static final AnvilDataSerializer INSTANCE = new AnvilDataSerializer();

    public Optional<CloudLevelData> load(CloudLevelData initialData, Path levelPath) throws IOException {
        Path levelDatPath = levelPath.resolve("level.dat");
        Path levelDatOldPath = levelPath.resolve("level.dat_old");

        if (Files.notExists(levelDatPath) && Files.notExists(levelDatOldPath)) {
            return Optional.empty();
        }

        try {
            CloudLevelData data = initialData.copy();
            loadData(data, levelDatPath);
            return Optional.of(data);
        } catch (IOException e) {
            CloudLevelData backupData = initialData.copy();
            loadData(backupData, levelDatOldPath);
            return Optional.of(backupData);
        }
    }

    private void loadData(CloudLevelData data, Path levelDatPath) throws IOException {
        NbtMap tag;
        try (NBTInputStream stream = NbtUtils.createReader(Files.newInputStream(levelDatPath))) {
            NbtMap root = (NbtMap) stream.readTag();
            tag = root.getCompound("Data");
        }

        data.setAdditionalData(tag);

        tag.listenForString("LevelName", data::setName);
        tag.listenForString("generatorName", s -> data.setGenerator(Identifier.parse(s)));
        tag.listenForString("generatorOptions", data::setGeneratorOptions);
        tag.listenForInt("thunderTime", data::setLightningTime);
        tag.listenForInt("Difficulty", id -> data.setDifficulty(Difficulty.fromId(id)));
        tag.listenForInt("GameType", id -> data.setGameMode(GameMode.from(id)));
        tag.listenForInt("serverChunkTickRange", data::setServerChunkTickRange);
        tag.listenForInt("NetherScale", data::setNetherScale);
        tag.listenForLong("currentTick", data::setCurrentTick);
        tag.listenForLong("LastPlayed", data::setLastPlayed);
        tag.listenForLong("RandomSeed", data::setSeed);
        tag.listenForLong("Time", data::setTime);
        if (tag.containsKey("SpawnX") && tag.containsKey("SpawnY") && tag.containsKey("SpawnZ")) {
            int x = tag.getInt("SpawnX");
            int y = tag.getInt("SpawnY");
            int z = tag.getInt("SpawnZ");
            data.setSpawn(Vector3i.from(x, y, z));
        }
        tag.listenForInt("Dimension", data::setDimension);
        tag.listenForInt("rainTime", data::setRainTime);
        tag.listenForFloat("rainLevel", data::setRainLevel);
        tag.listenForFloat("thunderLevel", data::setLightningLevel);
        readEndFightData(tag, data.getEndFightData());
        tag.listenForBoolean("hardcore", data::setHardcore);

        NbtMap gameRulesTag = tag.getCompound("GameRules");
        CloudGameRuleRegistry.get().getRules().forEach(rule -> readGameRule(data, gameRulesTag, rule));
    }

    private static void readEndFightData(NbtMap tag, CloudEndFightData data) throws IOException {
        tag.listenForBoolean("EndFightInitialized", data::setInitialized);
        tag.listenForBoolean("DragonKilled", data::setDragonKilled);
        tag.listenForBoolean("PreviouslyKilledDragon", data::setDragonPreviouslyKilled);
        tag.listenForInt("EndGatewayIndex", data::setGatewayIndex);
        tag.listenForInt("EndExitPortalY", data::setExitPortalY);
        if (tag.containsKey("EndDragonRespawnStage")) {
            data.setRespawnStage(parseRespawnStage(tag.getString("EndDragonRespawnStage")));
        }
        tag.listenForInt("EndDragonRespawnTime", data::setRespawnTime);
    }

    private static @Nullable EndDragonRespawnStage parseRespawnStage(String value) throws IOException {
        if (value.isEmpty()) {
            return null;
        }

        try {
            return EndDragonRespawnStage.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Unknown End dragon respawn stage: " + value, exception);
        }
    }

    private static <T extends Comparable<T>> void readGameRule(CloudLevelData data, NbtMap tag, GameRule<T> gameRule) {
        if (tag.containsKey(gameRule.getName())) {
            data.getGameRules().load(gameRule, gameRule.parse(tag.getString(gameRule.getName())));
        }
    }
}
