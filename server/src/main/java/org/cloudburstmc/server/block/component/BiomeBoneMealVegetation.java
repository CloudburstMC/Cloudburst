package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.biome.BiomeType;
import org.cloudburstmc.api.level.biome.BiomeTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BiomeBoneMealVegetation {

    private static final List<BlockType> DEFAULT = List.of(BlockTypes.POPPY, BlockTypes.POPPY, BlockTypes.DANDELION);
    private static final Map<BiomeType, List<BlockType>> FLOWERS = Map.ofEntries(
            Map.entry(BiomeTypes.CHERRY_GROVE, List.of(BlockTypes.PINK_PETALS)),
            Map.entry(BiomeTypes.FLOWER_FOREST, List.of(
                    BlockTypes.DANDELION, BlockTypes.POPPY, BlockTypes.ALLIUM, BlockTypes.AZURE_BLUET,
                    BlockTypes.RED_TULIP, BlockTypes.ORANGE_TULIP, BlockTypes.WHITE_TULIP,
                    BlockTypes.PINK_TULIP, BlockTypes.OXEYE_DAISY, BlockTypes.CORNFLOWER,
                    BlockTypes.LILY_OF_THE_VALLEY)),
            Map.entry(BiomeTypes.MEADOW, List.of(
                    BlockTypes.ALLIUM, BlockTypes.POPPY, BlockTypes.AZURE_BLUET,
                    BlockTypes.DANDELION, BlockTypes.CORNFLOWER, BlockTypes.OXEYE_DAISY)),
            Map.entry(BiomeTypes.PALE_GARDEN, List.of(BlockTypes.CLOSED_EYEBLOSSOM)),
            Map.entry(BiomeTypes.SWAMPLAND, List.of(BlockTypes.BLUE_ORCHID)),
            Map.entry(BiomeTypes.SWAMPLAND_MUTATED, List.of(BlockTypes.BLUE_ORCHID))
    );

    public static BlockType select(CloudLevel level, Vector3i position, RandomGenerator random) {
        @Nullable BiomeType biome = CloudBiomeRegistry.get().getType(level.getBiomeId(position.getX(), position.getY(), position.getZ()));
        List<BlockType> flowers = biome == null ? DEFAULT : FLOWERS.getOrDefault(biome, DEFAULT);
        return flowers.get(random.nextInt(flowers.size()));
    }
}
