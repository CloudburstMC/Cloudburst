package org.cloudburstmc.server.world.provider.leveldb.serializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.*;
import com.nukkitx.nbt.util.stream.LittleEndianDataInputStream;
import com.nukkitx.nbt.util.stream.LittleEndianDataOutputStream;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.world.WorldData;
import org.cloudburstmc.server.world.gamerule.GameRule;
import org.cloudburstmc.server.world.gamerule.GameRuleMap;
import org.cloudburstmc.server.world.provider.WorldDataSerializer;
import org.cloudburstmc.server.registry.GameRuleRegistry;
import org.cloudburstmc.server.utils.LoadState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Log4j2
@SuppressWarnings("UnstableApiUsage")
public class LevelDBDataSerializer implements WorldDataSerializer {
    public static final WorldDataSerializer INSTANCE = new LevelDBDataSerializer();

    private static final TypeReference<Map<String, Object>> OPTIONS_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final int VERSION = 8;

    @Override
    public LoadState load(WorldData data, Path levelPath, String levelId) throws IOException {
        Path levelDatPath = levelPath.resolve("world.dat");
        Path levelDatOldPath = levelPath.resolve("world.dat_old");

        if (Files.notExists(levelDatPath) && Files.notExists(levelDatOldPath)) {
            return LoadState.NOT_FOUND;
        }

        try {
            loadData(data, levelDatPath);
        } catch (IOException e) {
            // Attempt to load backup
            log.warn("Unable to load world.dat file, attempting to load backup.");
            loadData(data, levelDatOldPath);
        }
        return LoadState.LOADED;
    }

    @Override
    public void save(WorldData data, Path levelPath, String levelId) throws IOException {
        Path levelDatPath = levelPath.resolve("world.dat");
        Path levelDatOldPath = levelPath.resolve("world.dat_old");

        if (Files.exists(levelDatPath)) {
            Files.copy(levelDatPath, levelDatOldPath, StandardCopyOption.REPLACE_EXISTING);
        }

        saveData(data, levelDatPath);
    }

    private void saveData(WorldData data, Path levelDatPath) throws IOException {
        NbtMapBuilder tag = NbtMap.builder();
        if (data.getData() != null) {
            tag.putAll(data.getData());
        }
        tag.putString("LevelName", data.getName())
                .putString("FlatWorldLayers", data.getGeneratorOptions())
                .putString("generatorName", data.getGenerator().toString())
                .putInt("lightningTime", data.getLightningTime())
                .putInt("Difficulty", data.getDifficulty())
                .putInt("GameType", data.getGameType())
                .putInt("StorageVersion", VERSION)
                .putInt("serverChunkTickRange", data.getServerChunkTickRange())
                .putInt("NetherScale", data.getNetherScale())
                .putLong("currentTick", data.getCurrentTick())
                .putLong("LastPlayed", data.getLastPlayed())
                .putLong("RandomSeed", data.getRandomSeed())
                .putLong("Time", data.getTime())
                .putInt("SpawnX", data.getSpawn().getX())
                .putInt("SpawnY", data.getSpawn().getY())
                .putInt("SpawnZ", data.getSpawn().getZ())
                .putInt("Dimension", data.getDimension())
                .putInt("rainTime", data.getRainTime())
                .putFloat("rainLevel", data.getRainLevel())
                .putFloat("lightningLevel", data.getLightningLevel())
                .putBoolean("hardcore", data.isHardcore());

        // Gamerules - No idea why these aren't in a separate tag
        GameRuleMap gameRules = data.getGameRules();
        gameRules.forEach((gameRule, o) -> {
            String name = gameRule.getName().toLowerCase();
            if (gameRule.getValueClass() == Boolean.class) {
                tag.putBoolean(name, (boolean) o);
            } else if (gameRule.getValueClass() == Integer.class) {
                tag.putInt(name, (int) o);
            } else if (gameRule.getValueClass() == Boolean.class) {
                tag.putFloat(name, (float) o);
            }
        });

        byte[] tagBytes;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            nbtOutputStream.writeTag(tag.build());
            tagBytes = stream.toByteArray();
        }

        // Write
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(Files.newOutputStream(levelDatPath))) {
            stream.writeInt(VERSION);
            stream.writeInt(tagBytes.length);
            stream.write(tagBytes);
        }
    }

    private void loadData(WorldData data, Path levelDatPath) throws IOException {
        NbtMap tag;
        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(Files.newInputStream(levelDatPath));
             NBTInputStream nbtInputStream = new NBTInputStream(stream)) {

            int version = stream.readInt();
            if (version != VERSION) {
                throw new IOException("Incompatible world.dat version");
            }
            stream.readInt(); // Size
            tag = (NbtMap) nbtInputStream.readTag();
        }

        data.setData(tag);

        /*tag.listenForString("LevelName", data::setName);
        if (tag.contains("FlatWorldLayers")) {
            data.setGeneratorOptions(tag.getString("FlatWorldLayers"));
        }
        tag.listenForString("generatorName", s -> data.setGenerator(Identifier.fromString(s)));*/
        tag.listenForInt("lightningTime", data::setLightningTime);
        tag.listenForInt("Difficulty", data::setDifficulty);
        tag.listenForInt("GameType", data::setGameType);
        tag.listenForInt("serverChunkTickRange", data::setServerChunkTickRange);
        tag.listenForInt("NetherScale", data::setNetherScale);
        tag.listenForLong("currentTick", data::setCurrentTick);
        tag.listenForLong("LastPlayed", data::setLastPlayed);
        tag.listenForLong("RandomSeed", data::setRandomSeed);
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
        tag.listenForBoolean("Hardcore", data::setHardcore);

        GameRuleRegistry.get().getRules().forEach(rule -> {
            Object value = tag.get(rule.getName().toLowerCase());

            if (value instanceof Byte) {
                data.getGameRules().put((GameRule<Boolean>) rule, (byte) value != 0);
            } else if (value instanceof Integer) {
                data.getGameRules().put((GameRule<Integer>) rule, (int) value);
            } else if (value instanceof Float) {
                data.getGameRules().put((GameRule<Float>) rule, (float) value);
            }
        });
    }
}
