package org.cloudburstmc.server.level.generator.standard.finish;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.biome.Biome;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.registry.BiomeRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public class IceSnowFinisher implements Finisher {
    public static final Identifier ID = Identifier.fromString("cloudburst:ice_snow");

    @JsonProperty
    protected IntRange height;

    @Override
    public void init(long levelSeed, long localSeed, StandardGenerator generator) {
        Objects.requireNonNull(this.height, "height must be set!");
    }

    @Override
    public void finish(PRandom random, ChunkManager level, int blockX, int blockZ) {
        Biome biome = BiomeRegistry.get().getBiome(level.getChunk(blockX >> 4, blockZ >> 4).getBiome(blockX & 0xF, blockZ & 0xF));
        int y = level.getChunk(blockX >> 4, blockZ >> 4).getHighestBlock(blockX & 0xF, blockZ & 0xF);
        if (this.height.contains(y) && biome.canSnowAt(level, blockX, y + 1, blockZ)) {
            BlockState state = level.getBlockAt(blockX, y, blockZ, 0);
            if (state.getId() == BlockIds.WATER) {
                level.setBlockAt(blockX, y, blockZ, 0, BlockStates.ICE);
            } else if (y < 255 && state.getBehavior().isSolid(state)) {
                level.setBlockAt(blockX, y + 1, blockZ, 0, BlockStates.SNOW_LAYER);
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
