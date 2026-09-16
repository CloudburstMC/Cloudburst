package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.biome.BiomeIds;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BiomeBoneMealVegetation {

    private static final List<BlockType> DEFAULT = List.of(
            BlockTypes.POPPY, BlockTypes.POPPY, BlockTypes.DANDELION);
    private static final Map<Identifier, List<BlockType>> FLOWERS = Map.ofEntries(
            Map.entry(BiomeIds.CHERRY_GROVE, List.of(BlockTypes.PINK_PETALS)),
            Map.entry(BiomeIds.FLOWER_FOREST, List.of(
                    BlockTypes.DANDELION, BlockTypes.POPPY, BlockTypes.ALLIUM, BlockTypes.AZURE_BLUET,
                    BlockTypes.RED_TULIP, BlockTypes.ORANGE_TULIP, BlockTypes.WHITE_TULIP,
                    BlockTypes.PINK_TULIP, BlockTypes.OXEYE_DAISY, BlockTypes.CORNFLOWER,
                    BlockTypes.LILY_OF_THE_VALLEY)),
            Map.entry(BiomeIds.MEADOW, List.of(
                    BlockTypes.ALLIUM, BlockTypes.POPPY, BlockTypes.AZURE_BLUET,
                    BlockTypes.DANDELION, BlockTypes.CORNFLOWER, BlockTypes.OXEYE_DAISY)),
            Map.entry(BiomeIds.PALE_GARDEN, List.of(BlockTypes.CLOSED_EYEBLOSSOM)),
            Map.entry(BiomeIds.SWAMPLAND, List.of(BlockTypes.BLUE_ORCHID)),
            Map.entry(BiomeIds.SWAMPLAND_MUTATED, List.of(BlockTypes.BLUE_ORCHID))
    );

    public static BlockType select(CloudLevel level, Vector3i position, RandomGenerator random) {
        Identifier biome = CloudBiomeRegistry.get().getId(level.getBiomeId(position.getX(), position.getY(), position.getZ()));
        List<BlockType> flowers = FLOWERS.getOrDefault(biome, DEFAULT);
        return flowers.get(random.nextInt(flowers.size()));
    }
}
