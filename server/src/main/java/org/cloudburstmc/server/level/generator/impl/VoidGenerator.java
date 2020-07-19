package org.cloudburstmc.server.level.generator.impl;

import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.Generator;
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
        //no-op
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
