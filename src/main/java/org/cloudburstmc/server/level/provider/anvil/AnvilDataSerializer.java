package org.cloudburstmc.server.level.provider.anvil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.*;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.LevelData;
import org.cloudburstmc.server.level.gamerule.GameRuleMap;
import org.cloudburstmc.server.level.provider.LevelDataSerializer;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.LoadState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class AnvilDataSerializer implements LevelDataSerializer {
    public static final LevelDataSerializer INSTANCE = new AnvilDataSerializer();

    private static final TypeReference<Map<String, Object>> OPTIONS_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final int VERSION = 8;

    @Override
    public LoadState load(LevelData data, Path levelPath, String levelId) throws IOException {
        Path levelDatPath = levelPath.resolve("level.data");
        Path levelDatOldPath = levelPath.resolve("level.dat_old");

        if (Files.notExists(levelDatPath) && Files.notExists(levelDatOldPath)) {
            return LoadState.NOT_FOUND;
        }

        try {
            loadData(data, levelPath.resolve("level.data"));
        } catch (IOException e) {
            // Attempt to load backup
            loadData(data, levelPath.resolve("level.dat_old"));
        }
        return LoadState.LOADED;
    }

    @Override
    public void save(LevelData data, Path levelPath, String levelId) throws IOException {
        Path levelDatPath = levelPath.resolve("level.dat");
        Path levelDatOldPath = levelPath.resolve("level.dat_old");

        if (Files.exists(levelDatPath)) {
            Files.copy(levelDatPath, levelDatOldPath, StandardCopyOption.REPLACE_EXISTING);
        }

        saveData(data, levelDatPath);
    }

    private void saveData(LevelData data, Path levelDatPath) throws IOException {
        NbtMapBuilder tag = NbtMap.builder()
                .putString("LevelName", data.getName())
                .putString("generatorOptions", Bootstrap.JSON_MAPPER.writeValueAsString(data.getGeneratorOptions()))
                .putString("generatorName", data.getGenerator().toString())
                .putInt("thunderTime", data.getLightningTime())
                .putInt("Difficulty", data.getDifficulty())
                .putInt("GameType", data.getGameType())
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
                .putFloat("thunderLevel", data.getLightningLevel())
                .putBoolean("hardcore", data.isHardcore());

        NbtMapBuilder gameRulesTag = NbtMap.builder();
        GameRuleMap gameRules = data.getGameRules();
        gameRules.forEach((gameRule, o) -> {
            String name = gameRule.getName();
            gameRulesTag.putString(name, o.toString());
        });
        tag.putCompound("GameRules", gameRulesTag.build());

        // Write
        try (NBTOutputStream stream = NbtUtils.createWriter(Files.newOutputStream(levelDatPath))) {
            stream.writeTag(NbtMap.builder()
                    .putCompound("Data", tag.build())
                    .build());
        }
    }

    private void loadData(LevelData data, Path levelDatPath) throws IOException {
        NbtMap tag;
        try (NBTInputStream stream = NbtUtils.createReader(Files.newInputStream(levelDatPath))) {
            tag = (NbtMap) stream.readTag();
        }

        tag.listenForString("LevelName", data::setName);
        tag.listenForString("generatorName", s -> data.setGenerator(Identifier.fromString(s)));
        tag.listenForString("generatorOptions", data::setGeneratorOptions);
        tag.listenForInt("thunderTime", data::setLightningTime);
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
        tag.listenForFloat("thunderLevel", data::setLightningLevel);
        tag.listenForBoolean("hardcore", data::setHardcore);

        NbtMap gameRulesTag = tag.getCompound("GameRules");
        CloudGameRuleRegistry.get().getRules().forEach(rule -> {
            String value = gameRulesTag.getString(rule.getName());
            if (rule.getValueClass() == Boolean.class) {
                data.getGameRules().put((GameRule<Boolean>) rule, Boolean.valueOf(value));
            } else if (rule.getValueClass() == Integer.class) {
                data.getGameRules().put((GameRule<Integer>) rule, Integer.valueOf(value));
            } else if (rule.getValueClass() == Float.class) {
                data.getGameRules().put((GameRule<Float>) rule, Float.valueOf(value));
            }
        });
    }
}
