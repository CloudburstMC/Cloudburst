package org.cloudburstmc.server.world.generator.impl;

import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.world.ChunkManager;
import org.cloudburstmc.server.world.chunk.IChunk;
import org.cloudburstmc.server.world.generator.Generator;
import org.cloudburstmc.server.utils.Identifier;

/**
 * A basic generator that does nothing at all, resulting in a world of nothing but air.
 *
 * @author DaPorkchop_
 */
public final class VoidGenerator implements Generator {
    public static final Identifier ID = Identifier.from("minecraft", "void");

    public VoidGenerator(long seed, String options) {
        //no-op
    }

    @Override
    public void generate(PRandom random, IChunk chunk, int chunkX, int chunkZ) {
        int i = chunkX | chunkZ;
        if (((i | (i >> 31)) & ~1) == 0) {
            //both chunk coordinates are either 0 or 1
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlock(x, 64, z, 0, BlockStates.STONE);
                }
            }
        }
    }

    @Override
    public void populate(PRandom random, ChunkManager level, int chunkX, int chunkZ) {
        //no-op
    }

    @Override
    public void finish(PRandom random, ChunkManager level, int chunkX, int chunkZ) {
        //no-op
    }
}
