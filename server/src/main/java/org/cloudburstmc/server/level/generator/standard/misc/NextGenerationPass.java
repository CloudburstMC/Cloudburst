package org.cloudburstmc.server.level.generator.standard.misc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.generation.decorator.Decorator;
import org.cloudburstmc.server.level.generator.standard.population.Populator;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.random.RandomGenerator;

/**
 * Dummy generation pass to indicate where generation passes from the next layer down should be inserted.
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
    public void populate(RandomGenerator random, GenerationRegion region, int blockX, int blockZ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
