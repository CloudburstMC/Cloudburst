package org.cloudburstmc.server.level.generator.standard.finish;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
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
        CloudBiome biome = CloudBiomeRegistry.get().getBiome(level.getChunk(blockX >> 4, blockZ >> 4).getBiome(blockX & 0xF, blockZ & 0xF));
        int y = level.getChunk(blockX >> 4, blockZ >> 4).getHighestBlock(blockX & 0xF, blockZ & 0xF);
        if (this.height.contains(y) && biome.canSnowAt(level, blockX, y + 1, blockZ)) {
            BlockState state = level.getBlockState(blockX, y, blockZ, 0);
            if (state.getType().getId() == BlockIds.WATER) {
                level.setBlockState(blockX, y, blockZ, 0, BlockStates.ICE);
            } else if (y < 255 && CloudBlockRegistry.REGISTRY.getBehavior(state.getType(), BlockBehaviors.IS_SOLID)) {
                level.setBlockState(blockX, y + 1, blockZ, 0, BlockStates.SNOW_LAYER);
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}