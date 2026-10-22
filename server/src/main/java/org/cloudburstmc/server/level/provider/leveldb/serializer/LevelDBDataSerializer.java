package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.nbt.util.stream.LittleEndianDataInputStream;
import org.cloudburstmc.server.level.CloudEndFightData;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.EndDragonRespawnStage;
import org.cloudburstmc.server.level.gamerule.CloudGameRules;
import org.cloudburstmc.server.level.provider.CloudLevelDataSerializer;
import org.cloudburstmc.server.persistence.RecoverableFiles;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LevelDBDataSerializer implements CloudLevelDataSerializer {
    public static final CloudLevelDataSerializer INSTANCE = new LevelDBDataSerializer();

    private static final int STORAGE_VERSION = 10;
    private static final int STORAGE_VERSION_MIN = 8;

    @Override
    public Optional<CloudLevelData> load(CloudLevelData initialData, Path levelPath) throws IOException {
        return RecoverableFiles.read(levelPath.resolve("level.dat"), path -> {
            CloudLevelData data = initialData.copy();
            loadData(data, path);
            return data;
        });
    }

    @Override
    public void save(CloudLevelData data, Path levelPath) throws IOException {
        RecoverableFiles.write(levelPath.resolve("level.dat"), serialize(data), path -> {
            CloudLevelData previous = data.copy();
            loadData(previous, path);
            return previous;
        });
    }

    private byte[] serialize(CloudLevelData data) throws IOException {
        NbtMapBuilder tag = NbtMap.builder();
        tag.putAll(data.getAdditionalData());
        tag.putString("LevelName", data.getName())
                .putString("generatorOptions", data.getGeneratorOptions())
                .putString("generatorName", data.getGenerator().toString())
                .putInt("lightningTime", data.getLightningTime())
                .putInt("Difficulty", data.getDifficulty().getId())
                .putInt("GameType", data.getGameMode().getVanillaId())
                .putInt("StorageVersion", STORAGE_VERSION)
                .putInt("serverChunkTickRange", data.getServerChunkTickRange())
                .putInt("NetherScale", data.getNetherScale())
                .putLong("currentTick", data.getGameTime())
                .putLong("LastPlayed", data.getLastPlayed())
                .putLong("RandomSeed", data.getSeed())
                .putLong("Time", data.getClock().getTotalTicks())
                .putCompound("CloudburstClock", NbtMap.builder()
                        .putFloat("partialTick", data.getClock().getPartialTick())
                        .putFloat("rate", data.getClock().getRate())
                        .putBoolean("paused", data.getClock().isPaused()).build())
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
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            try (NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
                nbtOutputStream.writeTag(tag.build());
            }
            tagBytes = stream.toByteArray();
        }

        return ByteBuffer.allocate(8 + tagBytes.length).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(STORAGE_VERSION).putInt(tagBytes.length).put(tagBytes).array();
    }

    private void loadData(CloudLevelData data, Path levelDatPath) throws IOException {
        NbtMap tag;
        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(Files.newInputStream(levelDatPath));
             NBTInputStream nbtInputStream = new NBTInputStream(stream)) {

            int version = stream.readInt();
            if (version < STORAGE_VERSION_MIN || version > STORAGE_VERSION + 2) {
                throw new IOException("Incompatible level.dat version: " + version);
            }

            int length = stream.readInt();
            if (length < 0 || length != Files.size(levelDatPath) - 8) {
                throw new IOException("Invalid level.dat payload length: " + length);
            }

            if (!(nbtInputStream.readTag() instanceof NbtMap compound)) {
                throw new IOException("level.dat must contain an NBT compound");
            }

            tag = compound;
        }

        data.setAdditionalData(tag);

        tag.listenForString("LevelName", data::setName);
        readGenerator(tag, data);
        tag.listenForInt("lightningTime", data::setLightningTime);
        tag.listenForInt("Difficulty", id -> data.setDifficulty(Difficulty.fromId(id)));
        tag.listenForInt("GameType", id -> data.setGameMode(GameMode.from(id)));
        tag.listenForInt("serverChunkTickRange", data::setServerChunkTickRange);
        tag.listenForInt("NetherScale", data::setNetherScale);
        tag.listenForLong("currentTick", data::setGameTime);
        tag.listenForLong("LastPlayed", data::setLastPlayed);
        tag.listenForLong("RandomSeed", data::setSeed);
        tag.listenForLong("Time", data.getClock()::setTotalTicks);
        NbtMap clock = tag.getCompound("CloudburstClock");
        try {
            clock.listenForFloat("rate", data.getClock()::setRate);
            clock.listenForFloat("partialTick", data.getClock()::setPartialTick);
            clock.listenForBoolean("paused", data.getClock()::setPaused);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid daylight clock state", exception);
        }
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

    private static void readGenerator(NbtMap tag, CloudLevelData data) throws IOException {
        if (!tag.containsKey("generatorName")) {
            return;
        }

        if (!tag.containsKey("generatorName", NbtType.STRING)) {
            throw new IOException("Generator name must be a string");
        }

        data.setGenerator(Identifier.parse(tag.getString("generatorName")));
        String optionsKey = tag.containsKey("generatorOptions") ? "generatorOptions" : "FlatWorldLayers";
        if (tag.containsKey(optionsKey) && !tag.containsKey(optionsKey, NbtType.STRING)) {
            throw new IOException("Generator options must be a string");
        }

        data.setGeneratorOptions(tag.getString(optionsKey));
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
