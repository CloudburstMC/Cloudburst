package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.finish.Finisher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.random.RandomGenerator;

/**
 * Sets individual blocks of a chunk and its neighbors, allowing to generate larger structures and features.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize(using = PopulatorDeserializer.class)
public interface Populator extends Finisher {
    Populator[] EMPTY_ARRAY = new Populator[0];

    @Override
    default void finish(RandomGenerator random, ChunkManager level, int blockX, int blockZ) {
        this.populate(random, level, blockX, blockZ);
    }

    /**
     * Populates a given chunk.
     *
     * @param random an instance of {@link RandomGenerator} for generating random numbers, initialized with a seed based on chunk's position
     * @param level  a {@link ChunkManager} containing only a 3x3 square of generated chunks, centered around the chunk being populated
     * @param blockX the X coordinate of the block column to populate
     * @param blockZ the Z coordinate of the block column to populate
     */
    void populate(RandomGenerator random, ChunkManager level, int blockX, int blockZ);

    @Override
    Identifier getId();

    /**
     * Indicates that a specific {@link Populator} class should not be automatically registered to the {@link Finisher} registry as well.
     *
     * @author DaPorkchop_
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface SkipRegistrationAsFinisher {
    }
}
