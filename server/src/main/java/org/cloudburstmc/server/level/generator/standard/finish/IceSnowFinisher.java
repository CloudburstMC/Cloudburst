package org.cloudburstmc.server.level.generator.standard.finish;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import java.util.random.RandomGenerator;

@JsonDeserialize
public class IceSnowFinisher implements Finisher {
    public static final Identifier ID = Identifier.parse("cloudburst:ice_snow");
    private static final int MAX_FORMATION_LIGHT = 9;

    @JsonProperty
    protected IntRange height;
    private int seaLevel;

    @Override
    public void init(long levelSeed, long localSeed, StandardGenerator generator) {
        Objects.requireNonNull(this.height, "height must be set!");
        this.seaLevel = generator.getSeaLevel();
    }

    @Override
    public void finish(RandomGenerator random, GenerationRegion level, int blockX, int blockZ) {
        int localX = blockX & 0xF;
        int localZ = blockZ & 0xF;
        Chunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
        int surfaceY = chunk.getHighestBlock(localX, localZ);
        if (!this.height.contains(surfaceY)) {
            return;
        }

        CloudBiome biome = CloudBiomeRegistry.get().getBiome(chunk.getBiome(localX, surfaceY, localZ));
        if (biome == null) {
            return;
        }

        BlockState surfaceState = level.getBlockState(blockX, surfaceY, blockZ);
        if (surfaceState.getType() == BlockTypes.WATER
                && chunk.getBlockLight(localX, surfaceY, localZ) <= MAX_FORMATION_LIGHT
                && biome.coldEnoughToSnow(blockX, surfaceY, blockZ, this.seaLevel)) {
            surfaceState = BlockStates.ICE;
            level.setBlockState(blockX, surfaceY, blockZ, surfaceState);
        }

        int snowY = surfaceY + 1;
        if (!biome.hasPrecipitation()
                || chunk.getLevel().isOutsideBuildHeight(snowY)
                || chunk.getBlockLight(localX, snowY, localZ) > MAX_FORMATION_LIGHT
                || !biome.coldEnoughToSnow(blockX, snowY, blockZ, this.seaLevel)
                || level.getBlockState(blockX, snowY, blockZ) != BlockStates.AIR) {
            return;
        }

        if (BlockSupport.isFaceSturdy(CloudBlockRegistry.REGISTRY, surfaceState, Direction.UP, SupportType.FULL)) {
            level.setBlockState(blockX, snowY, blockZ, BlockStates.SNOW_LAYER);
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
