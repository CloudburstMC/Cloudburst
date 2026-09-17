package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.biome.Biome;
import org.cloudburstmc.api.level.biome.BiomeType;
import org.cloudburstmc.api.level.biome.BiomeTypes;
import org.cloudburstmc.api.registry.BiomeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.network.VanillaBiomeNetworkData;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static org.cloudburstmc.api.level.biome.BiomeTypes.*;

public class CloudBiomeRegistry implements BiomeRegistry {
    private static final Map<Identifier, CloudBiome> VANILLA_BIOMES = loadVanillaBiomes();
    private static final CloudBiomeRegistry INSTANCE = new CloudBiomeRegistry();

    private final Int2ObjectMap<CloudBiome> runtimeToBiomeMap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<BiomeType> runtimeToTypeMap = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<Identifier> idToRuntimeMap = new Object2IntLinkedOpenHashMap<>();
    private volatile boolean closed;

    private CloudBiomeRegistry() {
        this.registerVanillaBiomes();
    }

    public static CloudBiomeRegistry get() {
        return INSTANCE;
    }

    private void registerVanilla(@NonNull BiomeType type, int runtime) {
        CloudBiome biome = VANILLA_BIOMES.get(type.getId());
        Preconditions.checkArgument(biome != null, "Unknown vanilla biome type: %s", type);
        this.registerInternal(biome, runtime);
    }

    private synchronized void registerInternal(CloudBiome biome, int runtime) {
        this.checkClosed();
        Preconditions.checkArgument(runtime >= 0, "Runtime ID may not be negative!");
        Preconditions.checkState(!this.runtimeToTypeMap.containsKey(runtime), "Runtime ID already registered: %s", runtime);
        Preconditions.checkState(!this.idToRuntimeMap.containsKey(biome.getType().getId()), "Biome type already registered: %s", biome.getType());

        this.runtimeToBiomeMap.put(runtime, biome);
        this.runtimeToTypeMap.put(runtime, biome.getType());
        this.idToRuntimeMap.put(biome.getType().getId(), runtime);
    }

    public int getRuntimeId(CloudBiome biome) {
        return this.getRuntimeId(biome.getType());
    }

    public int getRuntimeId(BiomeType type) {
        return this.idToRuntimeMap.getOrDefault(type.getId(), -1);
    }

    @Override
    public @Nullable CloudBiome getBiome(BiomeType type) {
        return this.getBiome(this.getRuntimeId(type));
    }

    public @Nullable CloudBiome getBiome(int runtimeId) {
        return this.runtimeToBiomeMap.get(runtimeId);
    }

    @Override
    public Collection<Biome> values() {
        return List.copyOf(this.runtimeToBiomeMap.values());
    }

    public @Nullable BiomeType getType(int runtimeId) {
        return this.runtimeToTypeMap.get(runtimeId);
    }

    @Override
    public synchronized void close() throws RegistryException {
        this.checkClosed();
        this.closed = true;
    }

    private void checkClosed() {
        checkState(!this.closed, "Registration is already closed");
    }

    private static Map<Identifier, CloudBiome> loadVanillaBiomes() {
        Map<Identifier, CloudBiome> biomes = new LinkedHashMap<>();
        VanillaBiomeNetworkData.definitions().getDefinitions().forEach((name, data) -> {
            Identifier id = Identifier.parse(name);
            BiomeType type = BiomeTypes.get(id).orElseThrow(() -> new RegistryException("Unknown built-in biome type " + id));
            Set<Identifier> tags = data.getTags() == null ? Set.of() : Set.copyOf(data.getTags().stream().map(Identifier::parse).toList());
            biomes.put(id, new CloudBiome(type, tags, data.getTemperature(), data.getDownfall()));
        });

        return Map.copyOf(biomes);
    }

    private void registerVanillaBiomes() {
        this.registerVanilla(OCEAN, 0);
        this.registerVanilla(PLAINS, 1);
        this.registerVanilla(DESERT, 2);
        this.registerVanilla(EXTREME_HILLS, 3);
        this.registerVanilla(FOREST, 4);
        this.registerVanilla(TAIGA, 5);
        this.registerVanilla(SWAMPLAND, 6);
        this.registerVanilla(RIVER, 7);
        this.registerVanilla(HELL, 8);
        this.registerVanilla(THE_END, 9);
        this.registerVanilla(LEGACY_FROZEN_OCEAN, 10);
        this.registerVanilla(FROZEN_RIVER, 11);
        this.registerVanilla(ICE_PLAINS, 12);
        this.registerVanilla(ICE_MOUNTAINS, 13);
        this.registerVanilla(MUSHROOM_ISLAND, 14);
        this.registerVanilla(MUSHROOM_ISLAND_SHORE, 15);
        this.registerVanilla(BEACH, 16);
        this.registerVanilla(DESERT_HILLS, 17);
        this.registerVanilla(FOREST_HILLS, 18);
        this.registerVanilla(TAIGA_HILLS, 19);
        this.registerVanilla(EXTREME_HILLS_EDGE, 20);
        this.registerVanilla(JUNGLE, 21);
        this.registerVanilla(JUNGLE_HILLS, 22);
        this.registerVanilla(JUNGLE_EDGE, 23);
        this.registerVanilla(DEEP_OCEAN, 24);
        this.registerVanilla(STONE_BEACH, 25);
        this.registerVanilla(COLD_BEACH, 26);
        this.registerVanilla(BIRCH_FOREST, 27);
        this.registerVanilla(BIRCH_FOREST_HILLS, 28);
        this.registerVanilla(ROOFED_FOREST, 29);
        this.registerVanilla(COLD_TAIGA, 30);
        this.registerVanilla(COLD_TAIGA_HILLS, 31);
        this.registerVanilla(MEGA_TAIGA, 32);
        this.registerVanilla(MEGA_TAIGA_HILLS, 33);
        this.registerVanilla(EXTREME_HILLS_PLUS_TREES, 34);
        this.registerVanilla(SAVANNA, 35);
        this.registerVanilla(SAVANNA_PLATEAU, 36);
        this.registerVanilla(MESA, 37);
        this.registerVanilla(MESA_PLATEAU_STONE, 38);
        this.registerVanilla(MESA_PLATEAU, 39);
        this.registerVanilla(WARM_OCEAN, 40);
        this.registerVanilla(DEEP_WARM_OCEAN, 41);
        this.registerVanilla(LUKEWARM_OCEAN, 42);
        this.registerVanilla(DEEP_LUKEWARM_OCEAN, 43);
        this.registerVanilla(COLD_OCEAN, 44);
        this.registerVanilla(DEEP_COLD_OCEAN, 45);
        this.registerVanilla(FROZEN_OCEAN, 46);
        this.registerVanilla(DEEP_FROZEN_OCEAN, 47);
        this.registerVanilla(BAMBOO_JUNGLE, 48);
        this.registerVanilla(BAMBOO_JUNGLE_HILLS, 49);
        this.registerVanilla(SUNFLOWER_PLAINS, 129);
        this.registerVanilla(DESERT_MUTATED, 130);
        this.registerVanilla(EXTREME_HILLS_MUTATED, 131);
        this.registerVanilla(FLOWER_FOREST, 132);
        this.registerVanilla(TAIGA_MUTATED, 133);
        this.registerVanilla(SWAMPLAND_MUTATED, 134);
        this.registerVanilla(ICE_PLAINS_SPIKES, 140);
        this.registerVanilla(JUNGLE_MUTATED, 149);
        this.registerVanilla(JUNGLE_EDGE_MUTATED, 151);
        this.registerVanilla(BIRCH_FOREST_MUTATED, 155);
        this.registerVanilla(BIRCH_FOREST_HILLS_MUTATED, 156);
        this.registerVanilla(ROOFED_FOREST_MUTATED, 157);
        this.registerVanilla(COLD_TAIGA_MUTATED, 158);
        this.registerVanilla(REDWOOD_TAIGA_MUTATED, 160);
        this.registerVanilla(REDWOOD_TAIGA_HILLS_MUTATED, 161);
        this.registerVanilla(EXTREME_HILLS_PLUS_TREES_MUTATED, 162);
        this.registerVanilla(SAVANNA_MUTATED, 163);
        this.registerVanilla(SAVANNA_PLATEAU_MUTATED, 164);
        this.registerVanilla(MESA_BRYCE, 165);
        this.registerVanilla(MESA_PLATEAU_STONE_MUTATED, 166);
        this.registerVanilla(MESA_PLATEAU_MUTATED, 167);
        this.registerVanilla(SOULSAND_VALLEY, 178);
        this.registerVanilla(CRIMSON_FOREST, 179);
        this.registerVanilla(WARPED_FOREST, 180);
        this.registerVanilla(BASALT_DELTAS, 181);
        this.registerVanilla(JAGGED_PEAKS, 182);
        this.registerVanilla(FROZEN_PEAKS, 183);
        this.registerVanilla(SNOWY_SLOPES, 184);
        this.registerVanilla(GROVE, 185);
        this.registerVanilla(MEADOW, 186);
        this.registerVanilla(LUSH_CAVES, 187);
        this.registerVanilla(DRIPSTONE_CAVES, 188);
        this.registerVanilla(STONY_PEAKS, 189);
        this.registerVanilla(DEEP_DARK, 190);
        this.registerVanilla(MANGROVE_SWAMP, 191);
        this.registerVanilla(CHERRY_GROVE, 192);
        this.registerVanilla(PALE_GARDEN, 193);
        this.registerVanilla(SULFUR_CAVES, 194);
        this.registerVanilla(DAPPLED_FOREST, 195);
    }
}
