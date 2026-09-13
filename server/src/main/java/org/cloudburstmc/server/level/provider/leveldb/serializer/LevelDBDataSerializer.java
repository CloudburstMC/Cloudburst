package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.nbt.util.stream.LittleEndianDataInputStream;
import org.cloudburstmc.nbt.util.stream.LittleEndianDataOutputStream;
import org.cloudburstmc.server.level.CloudEndFightData;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.EndDragonRespawnStage;
import org.cloudburstmc.server.level.gamerule.CloudGameRules;
import org.cloudburstmc.server.level.provider.CloudLevelDataSerializer;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LevelDBDataSerializer implements CloudLevelDataSerializer {
    public static final CloudLevelDataSerializer INSTANCE = new LevelDBDataSerializer();

    private static final int STORAGE_VERSION = 10;
    private static final int STORAGE_VERSION_MIN = 8;

    @Override
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
            log.warn("Unable to load level.dat file, attempting to load backup.");
            CloudLevelData backupData = initialData.copy();
            loadData(backupData, levelDatOldPath);
            return Optional.of(backupData);
        }
    }

    @Override
    public void save(CloudLevelData data, Path levelPath) throws IOException {
        Path levelDatPath = levelPath.resolve("level.dat");
        Path levelDatOldPath = levelPath.resolve("level.dat_old");

        if (Files.exists(levelDatPath)) {
            Files.copy(levelDatPath, levelDatOldPath, StandardCopyOption.REPLACE_EXISTING);
        }

        saveData(data, levelDatPath);
    }

    private void saveData(CloudLevelData data, Path levelDatPath) throws IOException {
        NbtMapBuilder tag = NbtMap.builder();
        tag.putAll(data.getAdditionalData());
        tag.putString("LevelName", data.getName())
                .putString("FlatWorldLayers", data.getGeneratorOptions())
                .putString("generatorName", data.getGenerator().toString())
                .putInt("lightningTime", data.getLightningTime())
                .putInt("Difficulty", data.getDifficulty().getId())
                .putInt("GameType", data.getGameMode().getVanillaId())
                .putInt("StorageVersion", STORAGE_VERSION)
                .putInt("serverChunkTickRange", data.getServerChunkTickRange())
                .putInt("NetherScale", data.getNetherScale())
                .putLong("currentTick", data.getCurrentTick())
                .putLong("LastPlayed", data.getLastPlayed())
                .putLong("RandomSeed", data.getSeed())
                .putLong("Time", data.getTime())
                .putInt("SpawnX", data.getSpawn().getX())
                .putInt("SpawnY", data.getSpawn().getY())
                .putInt("SpawnZ", data.getSpawn().getZ())
                .putInt("Dimension", data.getDimension())
                .putInt("rainTime", data.getRainTime())
                .putFloat("rainLevel", data.getRainLevel())
                .putFloat("lightningLevel", data.getLightningLevel())
                .putBoolean("Hardcore", data.isHardcore());

        writeEndFightData(tag, data.getEndFightData());

        CloudGameRules gameRules = data.getGameRules();
        for (LevelGameRules.Entry<?> entry : gameRules) {
            writeGameRule(tag, entry);
        }

        byte[] tagBytes;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            nbtOutputStream.writeTag(tag.build());
            tagBytes = stream.toByteArray();
        }

        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(Files.newOutputStream(levelDatPath))) {
            stream.writeInt(STORAGE_VERSION);
            stream.writeInt(tagBytes.length);
            stream.write(tagBytes);
        }
    }

    private void loadData(CloudLevelData data, Path levelDatPath) throws IOException {
        NbtMap tag;
        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(Files.newInputStream(levelDatPath));
             NBTInputStream nbtInputStream = new NBTInputStream(stream)) {

            int version = stream.readInt();
            if (version < STORAGE_VERSION_MIN || version > STORAGE_VERSION + 2) {
                throw new IOException("Incompatible level.dat version: " + version);
            }
            stream.readInt(); // Size
            tag = (NbtMap) nbtInputStream.readTag();
        }

        data.setAdditionalData(tag);

        tag.listenForString("LevelName", data::setName);
        tag.listenForString("FlatWorldLayers", data::setGeneratorOptions);
        tag.listenForString("generatorName", value -> data.setGenerator(Identifier.parse(value)));
        tag.listenForInt("lightningTime", data::setLightningTime);
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
        tag.listenForFloat("lightningLevel", data::setLightningLevel);
        readEndFightData(tag, data.getEndFightData());
        tag.listenForBoolean("Hardcore", data::setHardcore);

        CloudGameRuleRegistry.get().getRules().forEach(rule -> {
            Object value = tag.get(rule.getName().toLowerCase(Locale.ROOT));

            if (value instanceof Byte byteValue) {
                loadBooleanGameRule(data, rule, byteValue != 0);
            } else if (value instanceof Integer || value instanceof Float) {
                loadGameRule(data, rule, value);
            }
        });
    }

    private static <T extends Comparable<T>> void writeGameRule(NbtMapBuilder tag, LevelGameRules.Entry<T> entry) {
        String name = entry.rule().getName().toLowerCase(Locale.ROOT);
        T value = entry.value();
        if (value instanceof Boolean booleanValue) {
            tag.putBoolean(name, booleanValue);
        } else if (value instanceof Integer integerValue) {
            tag.putInt(name, integerValue);
        } else if (value instanceof Float floatValue) {
            tag.putFloat(name, floatValue);
        }
    }

    private static void writeEndFightData(NbtMapBuilder tag, CloudEndFightData data) {
        tag.putBoolean("EndFightInitialized", data.isInitialized())
                .putBoolean("DragonKilled", data.isDragonKilled())
                .putBoolean("PreviouslyKilledDragon", data.isDragonPreviouslyKilled())
                .putInt("EndGatewayIndex", data.getGatewayIndex())
                .putString("EndDragonRespawnStage", data.getRespawnStage() == null ? "" : data.getRespawnStage().name())
                .putInt("EndDragonRespawnTime", data.getRespawnTime());

        if (data.getExitPortalY() != null) {
            tag.putInt("EndExitPortalY", data.getExitPortalY());
        }
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

    private static <T extends Comparable<T>> void loadBooleanGameRule(CloudLevelData data, GameRule<T> gameRule, boolean value) {
        loadGameRule(data, gameRule, value);
    }

    private static <T extends Comparable<T>> void loadGameRule(CloudLevelData data, GameRule<T> gameRule, Object value) {
        data.getGameRules().load(gameRule, gameRule.getValueClass().cast(value));
    }
}
