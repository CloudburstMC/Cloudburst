package org.cloudburstmc.server.level.generator.impl;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.Generator;

import java.util.random.RandomGenerator;

/**
 * A basic generator that does nothing at all, resulting in a world of nothing but air.
 */
public final class VoidGenerator implements Generator {
    public static final Identifier ID = Identifier.from("minecraft", "void");

    public VoidGenerator(long seed, String options) {
        //no-op
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public void generate(RandomGenerator random, Chunk chunk, int chunkX, int chunkZ) {
        int i = chunkX | chunkZ;
        if (((i | (i >> 31)) & ~1) == 0) {
            //both chunk coordinates are either 0 or 1
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(x, 64, z, BlockStates.STONE);
                }
            }
        }
    }

    @Override
    public void populate(RandomGenerator random, GenerationRegion region, int chunkX, int chunkZ) {
        //no-op
    }

    @Override
    public void finish(RandomGenerator random, GenerationRegion region, int chunkX, int chunkZ) {
        //no-op
    }
}
