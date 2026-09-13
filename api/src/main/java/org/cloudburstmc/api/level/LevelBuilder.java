package org.cloudburstmc.api.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;

import java.util.concurrent.CompletableFuture;

/**
 * Configures a level to load or create.
 *
 * <p>Existing persisted values take precedence over creation settings except
 * for the configured dimension. New levels default to a random seed, the
 * overworld dimension, and the server's fallback generator.
 */
public interface LevelBuilder {

    /**
     * Sets the seed used when creating the level.
     *
     * @param seed level seed
     * @return this builder
     */
    LevelBuilder seed(long seed);

    /**
     * Sets the dimension represented by the level.
     *
     * @param dimension dimension ID
     * @return this builder
     */
    LevelBuilder dimension(int dimension);

    /**
     * Sets the generator used when creating the level.
     *
     * @param generator registered generator ID
     * @return this builder
     */
    LevelBuilder generator(Identifier generator);

    /**
     * Sets generator-specific creation options.
     *
     * @param generatorOptions generator options, or {@code null} for none
     * @return this builder
     */
    LevelBuilder generatorOptions(@Nullable String generatorOptions);

    /**
     * Loads or creates the configured level.
     *
     * @return future completed with the initialized level
     */
    CompletableFuture<? extends Level> load();
}
