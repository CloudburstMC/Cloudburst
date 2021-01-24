package org.cloudburstmc.server.world.generator.standard.finish;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.world.ChunkManager;
import org.cloudburstmc.server.world.generator.standard.misc.GenerationPass;
import org.cloudburstmc.server.world.generator.standard.population.Populator;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Exactly identical to a {@link Populator}, but only runs after the chunk and its neighbors have been
 * populated.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize(using = FinisherDeserializer.class)
public interface Finisher extends GenerationPass {
    Finisher[] EMPTY_ARRAY = new Finisher[0];

    /**
     * Finishes a given chunk.
     *
     * @param random an instance of {@link PRandom} for generating random numbers, initialized with a seed based on chunk's position
     * @param level  a {@link ChunkManager} containing only a 3x3 square of populated chunks, centered around the chunk being finishes
     * @param blockX the X coordinate of the block column to finish
     * @param blockZ the Z coordinate of the block column to finish
     */
    void finish(PRandom random, ChunkManager level, int blockX, int blockZ);

    @Override
    Identifier getId();
}
