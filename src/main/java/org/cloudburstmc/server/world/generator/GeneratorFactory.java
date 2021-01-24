package org.cloudburstmc.server.world.generator;

/**
 * Creates {@link Generator} instances.
 *
 * @author DaPorkchop_
 */
@FunctionalInterface
public interface GeneratorFactory {
    /**
     * Creates a new {@link Generator} using the given seed and options.
     * <p>
     * The returned {@link Generator} must be ready to use before being returned.
     *
     * @param seed    the seed of the world being generated
     * @param options the options string for the generator, as configured in cloudburst.yml
     * @return a newly created {@link Generator}
     */
    Generator create(long seed, String options);
}
