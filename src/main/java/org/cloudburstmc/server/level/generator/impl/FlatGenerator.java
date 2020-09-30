package org.cloudburstmc.server.level.generator.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.Generator;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector.Entry;
import org.cloudburstmc.server.utils.Identifier;

/**
 * A basic generator for superflat worlds.
 *
 * @author DaPorkchop_
 */
public final class FlatGenerator implements Generator {
    public static final Identifier ID = Identifier.from("minecraft", "flat");

    private static final String DEFAULT_PRESET = "bedrock,3*dirt,grass";

    private final Entry[] layers;

    public FlatGenerator(long seed, String options) {
        if (options == null || options.isEmpty()) {
            options = DEFAULT_PRESET;
        }

        try {
            this.layers = Bootstrap.YAML_MAPPER.readValue(options, BlockSelector.class).entries().toArray(Entry[]::new);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(options, e);
        }
    }

    @Override
    public void generate(PRandom random, IChunk chunk, int chunkX, int chunkZ) {
        int y = 0;

        for (Entry layer : this.layers) {
            BlockState state = layer.state();
            for (int i = 0, size = layer.weight(); i < size; i++, y++) {
                for (int x = 15; x >= 0; x--) {
                    for (int z = 15; z >= 0; z--) {
                        chunk.setBlock(x, y, z, state);
                    }
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
