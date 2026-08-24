package org.cloudburstmc.server.level.generator.standard.finish;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.biome.CloudBiome;
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

    @JsonProperty
    protected IntRange height;

    @Override
    public void init(long levelSeed, long localSeed, StandardGenerator generator) {
        Objects.requireNonNull(this.height, "height must be set!");
    }

    @Override
    public void finish(RandomGenerator random, ChunkManager level, int blockX, int blockZ) {
        int y = level.getChunk(blockX >> 4, blockZ >> 4).getHighestBlock(blockX & 0xF, blockZ & 0xF);
        CloudBiome biome = CloudBiomeRegistry.get().getBiome(level.getChunk(blockX >> 4, blockZ >> 4).getBiome(blockX & 0xF, y, blockZ & 0xF));
        if (this.height.contains(y) && biome.canSnowAt(level, blockX, y + 1, blockZ)) {
            BlockState state = level.getBlockState(blockX, y, blockZ, 0);
            if (state.getType() == BlockTypes.WATER) {
                level.setBlockState(blockX, y, blockZ, 0, BlockStates.ICE);
            } else if (y < 255 && BlockSupport.isFaceSturdy(CloudBlockRegistry.REGISTRY, state, Direction.UP, SupportType.FULL)) {
                level.setBlockState(blockX, y + 1, blockZ, 0, BlockStates.SNOW_LAYER);
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
