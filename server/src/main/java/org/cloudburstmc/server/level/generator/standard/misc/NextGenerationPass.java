package org.cloudburstmc.server.level.generator.standard.misc;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.generation.decorator.Decorator;
import org.cloudburstmc.server.level.generator.standard.population.Populator;

import java.util.random.RandomGenerator;

/**
 * Dummy generation pass to indicate where generation passes from the next layer down should be inserted.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Decorator.SkipRegistrationAsPopulator
@Populator.SkipRegistrationAsFinisher
@JsonDeserialize
public final class NextGenerationPass implements Decorator, Populator {
    public static final Identifier ID = Identifier.parse("cloudburst:next");
    public static final NextGenerationPass INSTANCE = new NextGenerationPass();

    @Override
    public void decorate(RandomGenerator random, Chunk chunk, int x, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void populate(RandomGenerator random, ChunkManager level, int blockX, int blockZ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
